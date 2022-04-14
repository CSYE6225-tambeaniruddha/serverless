package com.lambda.csye6225;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final String EMAIL_SUBJECT="Verification needed";


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
		
		// Set reciepient
		//TO = "tambe.aniruddha3110@gmail.com";
		context.getLogger().log("Email set: " + username);
		
		// ********** Send Email **********
		
		this.sendEmail(context,record,username,token);
		context.getLogger().log("Email sent");
		
		// ********************************
		
		timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		
		context.getLogger().log("Invocation completed: " + timeStamp);
		
		return null;
	}
	
	public void sendEmail(Context context,String message,String username,String token) {
		
		/*
		try {
			context.getLogger().log("Inside sendEmail function");
		      AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
		    		  		//.withCredentials(new InstanceProfileCredentialsProvider(false))
		    		  		.withRegion(Regions.US_EAST_1).build();
		    
		      context.getLogger().log("Got client: "+client);
		      
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
		      
		      context.getLogger().log("Got request: "+request);
		      
		      client.sendEmail(request);
		      context.getLogger().log("Email sent!");
		      //System.out.println("Email sent!");
		    } catch (Exception ex) {
		    	context.getLogger().log("\"The email was not sent");
		      System.out.println("The email was not sent. Error message: " 
		          + ex.getMessage());
		    }
		*/       
        
        String emailRecipient = username;//(String) jsonObject.get("EmailAddress").getAsString();
        String accessToken = token;//(String) jsonObject.get("AccessToken").getAsString();
        
        logger.info("emailRecipient="+emailRecipient);
        logger.info("accessToken="+accessToken);
        
        String emailBody = "Thank you for registering at us\n.Please click on the below verification link to confirm your registration: \n";
        emailBody += "http://demo.aniruddhatambe.me/v1/verifyUserEmail?email="+emailRecipient+"&token="+accessToken;
        
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

