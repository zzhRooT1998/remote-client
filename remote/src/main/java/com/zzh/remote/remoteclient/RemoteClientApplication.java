package com.zzh.remote.remoteclient;

import com.zzh.remote.remoteclient.remote.EnableRemoteClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRemoteClients
public class RemoteClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemoteClientApplication.class, args);
    }

}
