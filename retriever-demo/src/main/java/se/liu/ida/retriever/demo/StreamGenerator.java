package se.liu.ida.retriever.demo;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;

public class StreamGenerator
{
	final public ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	final public Javalin app;

	final static Mustache TEMPERATURE_TEMPLATE = new DefaultMustacheFactory()
			.compile("templates/temperature-observation.mustache");
	final static Mustache HUMIDITY_TEMPLATE = new DefaultMustacheFactory()
			.compile("templates/humidity-observation.mustache");

	public StreamGenerator(){
		app = Javalin.create(config -> {
			config.staticFiles.add(staticFiles -> {
				staticFiles.hostedPath = "/catalog";        // change to host files on a subpath, like '/assets'
				staticFiles.directory = "/catalog";         // the directory where your files are located
				staticFiles.location = Location.CLASSPATH;  // Location.CLASSPATH (jar) or Location.EXTERNAL (file system)
			});
		});
	}

	public void run() {
		final StreamGenerator w = new StreamGenerator();
		w.createStream(HUMIDITY_TEMPLATE, "sensor1", 75, 70, 90, 1, 1.0);
		w.createStream(HUMIDITY_TEMPLATE, "sensor2", 80, 70, 90, 2, 0.5);
		w.createStream(TEMPERATURE_TEMPLATE, "sensor3", 20, 18, 22, 0.2, 1.0);
		w.createStream(TEMPERATURE_TEMPLATE, "sensor4", 20, 17, 23, 0.1, 0.5);
		
		w.app.start(7070);
	}

	public void createStream( final Mustache template,
		                      final String sensorId,
	                          final double startValue,
	                          final double minValue,
	                          final double maxValue,
							  final double valueIncrement,
	                          final double streamRate ) {
		final Set<WsContext> sessions = ConcurrentHashMap.newKeySet();
		
		// Landing page and websocket endpoints
		final String restEndpoint = "/" + sensorId + "/";
		final String wsEndpoint = "/ws/" + sensorId;

		// Should return VoCaLS catalog info
		app.get( restEndpoint, ctx -> ctx.result( "VoCaLS cat for stream on " + wsEndpoint ) );

		// WS endpoint
		app.ws( wsEndpoint, ws -> {
			ws.onConnect( ctx -> sessions.add(ctx) );
			ws.onClose( ctx -> sessions.remove(ctx) );
			ws.onError( ctx -> sessions.remove(ctx) );
		} );

		// Values
		final AtomicInteger obsId = new AtomicInteger(0);
		final DoubleAdder value = new DoubleAdder();
		value.add(startValue);
		final AtomicBoolean increasing = new AtomicBoolean(value.doubleValue() < maxValue ? true : false);
		
		// Ticker to generate data
		final ScheduledFuture<?> ticker = scheduler.scheduleAtFixedRate( () -> {
			// Increasing or decreasing 
			if ( value.doubleValue() >= maxValue) increasing.set(false);
			else if ( value.doubleValue() <= minValue ) increasing.set(true);

			// Update value
			if(increasing.get()) value.add(valueIncrement);
			else value.add(-valueIncrement);

			final String data = renderObservation(
				template,
				obsId.getAndIncrement(),
				sensorId,
				Instant.now(),
				value.doubleValue()
			);
			System.err.println(data);
			// Broadcast data to all current sessions
			for ( WsContext s : sessions ) {
				if ( s.session.isOpen() ) s.send(data);
				else sessions.remove(s);
			}
		}, 0, (long) (1000/streamRate), TimeUnit.MILLISECONDS );

		app.events( e -> e.serverStopped( () -> {
			ticker.cancel(true);
			scheduler.shutdownNow();
		} ) );
	}

	public static String renderObservation( final Mustache template,
		                                    final int obs,
	                                        final String sensor,
	                                        final Instant timestamp,
	                                        final double value ) {
		final String time = DateTimeFormatter.ISO_INSTANT.format(timestamp);
		final Map<String, Object> map = Map.of(
			"obs", obs,
			"sensor", sensor,
			"ts", time,
			"value", String.format(java.util.Locale.ROOT, "%.2f", value) 
		);
		
		final StringWriter out = new StringWriter();
		try {
			template.execute(out, map).flush();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return out.toString();
	}
}