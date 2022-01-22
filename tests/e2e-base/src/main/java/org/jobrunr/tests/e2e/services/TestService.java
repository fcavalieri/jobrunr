package org.jobrunr.tests.e2e.services;

import org.jobrunr.jobs.annotations.Job;

import java.util.UUID;

public class TestService {

    public void doWork() {
        System.out.println("This is a test service");
    }

    public void doWork(UUID id) {
        System.out.println("This is a test service " + id.toString());
    }

    @Job(name = "Recurring-Job-Test", retries = 10)
    public void doWorkThatFails() throws InterruptedException {
        Thread.sleep(5000);
        throw new RuntimeException("Whoopsie, an error occcured");
    }
}
