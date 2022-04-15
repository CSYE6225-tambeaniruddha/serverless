package com.lambda.csye6225;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.codec.Base64;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

@SuppressWarnings("deprecation")
public class LogEvent implements RequestHandler<SNSEvent, Object>{

	/*
	// ************* SES Email variables ************
	static final String FROM = "sender@demo.aniruddhatambe.me";
	static String TO = "recipient@example.com";
	static final String CONFIGSET = "ConfigSet";
	static final String SUBJECT = "Amazon SES(AWS SDK for Java) - Verify account";
	static final String HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>"
	      + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
	      + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>" 
	      + "AWS SDK for Java</a>";
	static final String TEXTBODY = "This email was sent through Amazon SES "
	      + "using the AWS SDK for Java.";
*/
	private static final Logger logger = LoggerFactory.getLogger(LogEvent.class);

    private static final String EMAIL_SUBJECT="Email verification request \\n \\n ";

    private static final String SENDER_EMAIL = "sender@demo.aniruddhatambe.me";//System.getenv("SenderEmail");
	
	@Override
	public Object handleRequest(SNSEvent request, Context context) {
		
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		
		context.getLogger().log("Invocation started: " + timeStamp);
		context.getLogger().log("Request is NULL: " + (request == null));
		context.getLogger().log("Number of records:: " + request.getRecords().size());
		
		String record = request.getRecords().get(0).getSNS().getMessage();
		
		context.getLogger().log("Record message: " + record);
		
		// *********** Decode ***********
		
		byte[] decodedBytes = Base64.decode(record.getBytes());
	 	String decodedMessage = new String(decodedBytes);
	 	
	 	String token = decodedMessage.split(";", 2)[0];
		String username = decodedMessage.split(";", 2)[1];
		
		context.getLogger().log("Email set: " + username);
		
		// ********* Check if mail is sent **************
		
		AmazonDynamoDB dynamodbClient = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(dynamodbClient);
		Table table = dynamoDB.getTable("Account");
		Item item = table.getItem("username",username,"token",token);
		
		if(item.get("messageSent") != null) {
			if(item.get("messageSent").equals("YES")) {
				context.getLogger().log("DUPLICATE EMAIL NOT SENT!!!!!!!!!");
				return null;
			}
		}
		
		
		// *************** Send Email ******************
		
		this.sendEmail(context,record,username,token);
		context.getLogger().log("Email sent");
		
		
		// ************** Mark as sent *************
			
		// Update item
		UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("Id", 121)
                .withUpdateExpression("set #na = :val1").withNameMap(new NameMap().with("#na", "messageSent"))
                .withValueMap(new ValueMap().withString(":val1", "YES")).withReturnValues(ReturnValue.ALL_NEW);

		UpdateItemOutcome outcome = table.updateItem(updateItemSpec);

		// ********************************
		
		timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		
		context.getLogger().log("Invocation completed: " + timeStamp);
		
		return null;
	}
	
	public void sendEmail(Context context,String message,String username,String token) {     
        
        String emailRecipient = username;//(String) jsonObject.get("EmailAddress").getAsString();
        String accessToken = token;//(String) jsonObject.get("AccessToken").getAsString();
        
        logger.info("emailRecipient="+emailRecipient);
        logger.info("accessToken="+accessToken);
        
        String emailBody =  " This email address is associated with the management account for a new organization, created . "+
        		"To invite or create accounts in your organization, you must first verify your email address by clicking the following link.\n \n "+ 
        		"This link expires 5 mins after the verification request was sent. \n\n"+"Access Token: "+token + "\nUsername: "+username+""+"\nLink: http://demo.aniruddhatambe.me/v1/verifyUserEmail?email="+emailRecipient+"&token="+accessToken +
        		"\n \n After you verify your email address, you can learn how to build your organization by reviewing the tutorial Creating and Configuring an organization and enable services that work with Organizations."+
        		" You can also review a collection of resources to assist you with your multi-account environment.";
        //emailBody += "http://demo.aniruddhatambe.me/v1/verifyUserEmail?email="+emailRecipient+"&token="+accessToken;
        
        Content content = new Content().withData(emailBody);
        Body body = new Body().withText(content);
        try {
        	logger.info("Before AmazonSimpleEmailService");
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard()
                            .withRegion(Regions.US_EAST_1).build();
            logger.info("Before SendEmailRequest");
            SendEmailRequest emailRequest = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(emailRecipient))
                    .withMessage(new Message()
                            .withBody(body)
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(EMAIL_SUBJECT)))
                    .withSource(SENDER_EMAIL);
            client.sendEmail(emailRequest);
            logger.info("MAIL SENT!!!!!!!!!!!!!!!!!!!!!!");
            
        } catch (Exception ex) {
        	logger.info("Error!");
        	ex.printStackTrace();
        }
        logger.info("Skipped?");

	}

	
}

