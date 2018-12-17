import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

public class Email implements RequestHandler<SNSEvent,String> {
    AmazonDynamoDB clientDynamoDB;
    AmazonSimpleEmailService clientEmail;
    public String handleRequest(SNSEvent snsEvent, Context context) {
        context.getLogger().log("Lambda Function started");
        try {
            clientDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion("us-east-1").build();
            clientEmail = AmazonSimpleEmailServiceClientBuilder.standard().withRegion("us-east-1").build();
            DynamoDB dynamoDB = new DynamoDB(clientDynamoDB);
            String uuid = UUID.randomUUID().toString();
            Table table = dynamoDB.getTable(System.getenv("TABLENAME"));
            for(SNSEvent.SNSRecord record: snsEvent.getRecords()) {
                context.getLogger().log("Found SNS record");
                SNSEvent.SNS sns = record.getSNS();
                String email = sns.getMessage();
                Date todayCal = Calendar.getInstance().getTime();
                SimpleDateFormat crunchifyFor = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
                String curTime = crunchifyFor.format(todayCal);
                Date curDate = crunchifyFor.parse(curTime);
                Long epoch = curDate.getTime();
                context.getLogger().log("Epoch value: "+epoch.toString());
                QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("username = :vusername").withFilterExpression("ttl_timestamp > :vtimestamp")
                        .withValueMap(new ValueMap()
                                .withString(":vusername",email)
                                .withString(":vtimestamp",epoch.toString()));
                ItemCollection<QueryOutcome> items = table.query(querySpec);
                Iterator<Item> iterator = items.iterator();
                if(iterator.hasNext() == false) {
                        context.getLogger().log("No entry found for username:"+email);
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.MINUTE, Integer.parseInt(System.getenv("TTL")));
                        Date today = cal.getTime();
                        SimpleDateFormat crunchifyFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
                        String currentTime = crunchifyFormat.format(today);
                        String link = System.getenv("DOMAIN_NAME")+"/"+ uuid;
                        Date date = crunchifyFormat.parse(currentTime);
                        Long epochTime = date.getTime();
                        Item item = new Item();
                        item.withPrimaryKey("username", email);
                        item.with("ttl_timestamp", epochTime.toString());
                        item.with("Subject", "Password Reset Link");
                        item.with("link", link);
                        context.getLogger().log("Logging time:" + epochTime.toString());
                        PutItemOutcome outcome = table.putItem(item);
                        SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(email)).withMessage(new Message()
                                .withBody(new Body()
                                        .withText(new Content()
                                                .withCharset("UTF-8").withData("Password reset Link:" + link)))
                                .withSubject(new Content()
                                        .withCharset("UTF-8").withData("Password Reset Link")))
                                .withSource(System.getenv("FROM_EMAIL"));
                        clientEmail.sendEmail(request);
                        context.getLogger().log("Email sent!");
                }
                else{
                    Item item = iterator.next();
                    context.getLogger().log("Entry found:");
                    context.getLogger().log("username:"+item.getString("username"));
                    context.getLogger().log("timestamp:"+item.getString("ttl_timestamp")+" current:"+epoch.toString());
                }
            }
        } catch (Exception ex) {
            context.getLogger().log("Error message: " + ex.getMessage()+"stack: "+ex.getStackTrace()[ex.getStackTrace().length -1].getLineNumber());
            context.getLogger().log(ex.getStackTrace()[ex.getStackTrace().length -1].getFileName());

        }
        return null;
    }
}
