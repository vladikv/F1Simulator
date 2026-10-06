package com.f1sim.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class OpenF1ClientTest {

    @Test
    @DisplayName("Rate limiter allows the first 28 requests immediately without delay")
    void firstRequestsGoThroughImmediately() throws Exception {
        OpenF1Client client = new OpenF1Client(RestClient.builder().build());

        // Access the private rate-limit method via reflection, since it's an
        // internal implementation detail with no public API — we're testing
        // the guard logic itself, not going through a real HTTP call.
        var method = OpenF1Client.class.getDeclaredMethod("awaitRateLimitSlot");
        method.setAccessible(true);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 28; i++) {
            method.invoke(client);
        }
        long elapsed = System.currentTimeMillis() - start;

        // 28 calls should all pass through without hitting the wait branch
        assertTrue(elapsed < 1000, "First 28 requests should not be throttled, took " + elapsed + "ms");
    }

    @Test
    @DisplayName("The 29th request within the window is delayed")
    void twentyNinthRequestIsDelayed() throws Exception {
        OpenF1Client client = new OpenF1Client(RestClient.builder().build());

        var method = OpenF1Client.class.getDeclaredMethod("awaitRateLimitSlot");
        method.setAccessible(true);

        for (int i = 0; i < 28; i++) {
            method.invoke(client);
        }

        long start = System.currentTimeMillis();
        method.invoke(client); // this is the 29th call — should wait
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed > 0, "29th request should have been delayed, but returned instantly");
    }
}