package io.openems.ojalgo;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import com.github.sh0nk.matplotlib4j.PythonExecutionException;

public class OjalgoApp {

	public static void main(String[] args) throws IOException, PythonExecutionException {
		Instant now = Instant.now();

		OjalgoTest ojalgoTest = new OjalgoTest();
		ojalgoTest.run();

		System.out.println("Time: " + Duration.between(now, Instant.now()).toMillis() + "ms");
	}

}
