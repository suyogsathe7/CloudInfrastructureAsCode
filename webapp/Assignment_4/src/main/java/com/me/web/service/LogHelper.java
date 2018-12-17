package com.me.web.service;

import org.springframework.stereotype.Service;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@Service
public class LogHelper {

    Logger logger = Logger.getLogger("MyLog");
    FileHandler fh;


    //@Value("${fileloc}")
    String logLocation="/opt/tomcat/logs/csye6225.log";


    public void logInfoEntry(String msg) throws Exception{
        fh = new FileHandler(logLocation);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);            // the following statement is used to log any messages
        logger.info(msg);
        fh.close();
    }

    /*
    public static void logErrorEntry(String msg) throws Exception{
        fh = new FileHandler("/opt/tomcat/logs/csye6225.log");
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);            // the following statement is used to log any messages
        logger.log(new Level(""),msg);
    }*/
}
