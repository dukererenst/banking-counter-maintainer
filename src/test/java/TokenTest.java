import com.counter.maintainer.TokenMaintenanceApp;
import com.counter.maintainer.data.contracts.*;
import com.counter.maintainer.repository.CounterRepository;
import com.counter.maintainer.service.TokenService;
import com.counter.maintainer.service.TokenServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.counter.maintainer.service.ProcessTimeConstants.DEPOSIT_TIME_IN_SEC;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TokenMaintenanceApp.class)
public class TokenTest
{
    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TokenService tokenService;

    private static final long SEC =1000;


    @Test
    public void createTokenForNewAndOldCustomer() throws InterruptedException {
        Token tokenReq = getFakeToken(ServicePriority.PREMIUM, TokenType.WITHDRAW);
        Token newCustomerToken = tokenService.createToken(tokenReq);
        Assert.assertTrue(newCustomerToken.getCustomerId()>0);
        Assert.assertTrue(newCustomerToken.getTokenId()>0);
        Assert.assertTrue(newCustomerToken.getCounterId()>0);
        Assert.assertTrue(newCustomerToken.getStatus()==TokenStatus.QUEUED);
        Assert.assertTrue(tokenService.getToken(newCustomerToken.getTokenId()).getStatus() == TokenStatus.QUEUED);
        Thread.sleep(15 * SEC);

        Assert.assertTrue(tokenService.getToken(newCustomerToken.getTokenId()).getStatus() == TokenStatus.COMPLETED);


        long customerId = newCustomerToken.getCustomerId();
        tokenReq.setCustomerId(customerId);
        Token existingCustomerToken = tokenService.createToken(tokenReq);

        Assert.assertTrue(existingCustomerToken.getCustomerId() == customerId);
        Assert.assertTrue(tokenService.getToken(existingCustomerToken.getTokenId()).getStatus() == TokenStatus.QUEUED);
        Thread.sleep(15 * SEC);

        Assert.assertTrue(tokenService.getToken(existingCustomerToken.getTokenId()).getStatus() == TokenStatus.COMPLETED);



    }

    public static Token getFakeToken(ServicePriority servicePriority) {
        Token token = new Token();
        Customer customer = new Customer();
        Address address = new Address();
        address.setState("AP");
        address.setCity("HYD");
        address.setCountry("INDIA");
        address.setStreetName("ROAD No.1");
        address.setZipCode("500034");
        customer.setAddress(address);
        customer.setPhoneNumber("123456789");
        customer.setName("Dummy Customer");
        token.setCustomer(customer);
        token.setServicePriority(servicePriority);
        token.setTokenType(TokenType.WITHDRAW);
        return  token;
    }

    public static Token getFakeToken(ServicePriority servicePriority, long tokenId) {
        Token token = getFakeToken( servicePriority);
        token.setTokenId(tokenId);
        return token;
    }

    public static Token getFakeToken(ServicePriority servicePriority, TokenType tokenType) {
        Token token = getFakeToken( servicePriority);
        token.setTokenType(tokenType);
        return token;
    }


}
