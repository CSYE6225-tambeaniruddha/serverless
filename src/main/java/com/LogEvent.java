package com;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.springframework.security.crypto.codec.Base64;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
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

public class LogEvent implements RequestHandler<SNSEvent, Object>{

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
	 	
	 	String token = decodedMessage.split(":", 2)[0];
		String username = decodedMessage.split(":", 2)[1];
		
		// Set reciepient
		TO = username;
		
		// ********** Send Email **********
		
		this.sendEmail();
		
		// ********************************
		
		timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		
		context.getLogger().log("Invocation completed: " + timeStamp);
		
		return null;
	}
	
	public void sendEmail() {
		
		try {
		      AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
		    		  		.withCredentials(new InstanceProfileCredentialsProvider(false))
		    		  		.withRegion(Regions.US_EAST_1).build();
		      
		      
		      SendEmailRequest request = new SendEmailRequest()
		          .withDestination(
		              new Destination().withToAddresses(TO))
		          .withMessage(new Message()
		              .withBody(new Body()
		                  .withHtml(new Content()
		                      .withCharset("UTF-8").withData(HTMLBODY))
		                  .withText(new Content()
		                      .withCharset("UTF-8").withData(TEXTBODY)))
		              .withSubject(new Content()
		                  .withCharset("UTF-8").withData(SUBJECT)))
		          .withSource(FROM)
		          // Comment or remove the next line if you are not using a
		          // configuration set
		          .withConfigurationSetName(CONFIGSET);
		      
		      client.sendEmail(request);
		      //System.out.println("Email sent!");
		    } catch (Exception ex) {
		      System.out.println("The email was not sent. Error message: " 
		          + ex.getMessage());
		    }
	}

	
}
