package flashsalesystem.paymentservice.controllers;

import flashsalesystem.paymentservice.enums.PaymentResult;
import flashsalesystem.paymentservice.services.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest
{
    private static final String PAYMENT_REQUEST_JSON = "{\"orderId\": \"order-1\", \"amountDue\": 19.98}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void processPayment_returns200_whenSuccess() throws Exception
    {
        when(paymentService.processPayment("order-1", 19.98)).thenReturn(PaymentResult.SUCCESS);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void processPayment_returns409_whenPaymentFailed() throws Exception
    {
        when(paymentService.processPayment("order-1", 19.98)).thenReturn(PaymentResult.PAYMENT_FAILED);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void processPayment_returns502_whenProcessorError() throws Exception
    {
        when(paymentService.processPayment("order-1", 19.98)).thenReturn(PaymentResult.PROCESSOR_ERROR);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST_JSON))
                .andExpect(status().isBadGateway());
    }

    @Test
    void processPayment_returns500_whenUnknownError() throws Exception
    {
        when(paymentService.processPayment("order-1", 19.98)).thenReturn(PaymentResult.UNKNOWN_ERROR);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST_JSON))
                .andExpect(status().isInternalServerError());
    }
}
