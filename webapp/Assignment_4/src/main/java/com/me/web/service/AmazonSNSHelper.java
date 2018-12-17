package com.me.web.service;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AmazonSNSHelper {
    AmazonSNSClient snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.standard().withRegion("us-east-1").build();

    @Value("${snsName}")
    String snsName;

    public void publish(String username){
        //publish to an SNS topic
        String msg = username;
        ListTopicsResult topicsResult = snsClient.listTopics();
        List<Topic> topicList = topicsResult.getTopics();
        Topic reset = null;//= topicList.get(topicList.indexOf("reset"));
        for(Topic topic: topicList) {
            if (topic.getTopicArn().contains(snsName))
                reset = topic;
        }
        if(reset != null) {
            PublishRequest publishRequest = new PublishRequest(reset.getTopicArn(), msg);
            PublishResult publishResult = snsClient.publish(publishRequest);
//print MessageId of message published to SNS topic
            System.out.println("MessageId - " + publishResult.getMessageId());
        }
        else{
            System.out.println("Topic not found");
        }
    }

}
