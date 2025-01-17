/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import static com.salesforce.emp.connector.LoginHelper.login;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.eclipse.jetty.util.ajax.JSON;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;

import com.salesforce.emp.connector.TopicSubscription;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

/**
 * An example of using the EMP connector using login credentials
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class LoginExample {

    // More than one thread can be used in the thread pool which leads to parallel processing of events which may be acceptable by the application
    // The main purpose of asynchronous event processing is to make sure that client is able to perform /meta/connect requests which keeps the session alive on the server side
    private static final ExecutorService workerThreadPool = Executors.newFixedThreadPool(1);

    public static void main(String[] argv) throws Exception, IOException {
        if (argv.length < 3 || argv.length > 4) {
            System.err.println("Usage: LoginExample username password topic [replayFrom]");
            System.exit(1);
        }
        long replayFrom = EmpConnector.REPLAY_FROM_TIP;
        if (argv.length == 4) {
            replayFrom = Long.parseLong(argv[3]);
        }

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return login(argv[0], argv[1]);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                System.exit(1);
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();
        
        Consumer<Map<String, Object>> consumer = event -> workerThreadPool.submit(() -> {

            try{
                postRequest(JSON.toString(event));
            }
            catch(Exception e){
                
            }
        });

        EmpConnector connector = new EmpConnector(params);

        connector.setBearerTokenProvider(tokenProvider);

        connector.start().get(5, TimeUnit.SECONDS);

        TopicSubscription subscription = connector.subscribe(argv[2], replayFrom, consumer).get(5, TimeUnit.SECONDS);

        System.out.println(String.format("Subscribed: %s", subscription));
        
    }

    public static void postRequest(String event) throws Exception {

        final String POST_PARAMS = event;

        URL obj = new URL("https://webhook.site/a3943e2a-5b1f-43cb-a7ad-d1c15243d8d8");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        //postConnection.setRequestProperty("userId", "a1bcdefgh");
        //postConnection.setRequestProperty("Content-Type", "application/json");
    
        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();
        
        postConnection.getResponseCode();
    }
}
