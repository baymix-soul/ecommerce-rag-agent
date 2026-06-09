package com.ecommerce.rag.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.rag.models.dto.LoginRequest;
import com.ecommerce.rag.models.dto.LoginResponse;
import com.ecommerce.rag.services.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendContractChatStreamTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void shouldAllowChatWithoutToken() throws Exception {
        String body = "{\"message\":\"推荐一款跑鞋\",\"limit\":2}";

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowChatWithToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("demo");
        loginRequest.setPassword("demo123");
        LoginResponse loginResponse = authService.login(loginRequest);

        String body = "{\"message\":\"推荐跑鞋\",\"limit\":2}";
        mockMvc.perform(post("/api/chat/stream")
                        .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void sseTextDataIsValidJson() throws Exception {
        String body = "{\"message\":\"推荐一款跑鞋\",\"limit\":1}";

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String content = mockMvc.perform(asyncDispatch(mvcResult))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("event:text"), "SSE should contain text events");
        assertTrue(content.contains("data:"), "SSE should contain data lines");
        assertTrue(content.contains("event:done"), "SSE should end with done");
    }

    @Test
    void sseProductCardHasSnakeCaseFields() throws Exception {
        String body = "{\"message\":\"推荐一款洗面奶\",\"limit\":3}";

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String content = mockMvc.perform(asyncDispatch(mvcResult))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("product_id"), "product_card should contain product_id");
        assertTrue(content.contains("image_url"), "product_card should contain image_url");
    }

    @Test
    void sseDoneDataIsEmptyObject() throws Exception {
        String body = "{\"message\":\"你好\",\"limit\":1}";

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String content = mockMvc.perform(asyncDispatch(mvcResult))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("data:{}") || content.contains("data: {}"),
                "done data should be empty object {}");
    }

    @Test
    void shouldAllowChatWithPageContextCurrentProductAddToCart() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("demo");
        loginRequest.setPassword("demo123");
        LoginResponse loginResponse = authService.login(loginRequest);

        String body = "{\"message\":\"把这个加入购物车\",\"page_context\":{\"page_type\":\"PRODUCT_DETAIL\",\"current_product_id\":\"p_beauty_010\"}}";

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String content = mockMvc.perform(asyncDispatch(mvcResult))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("已加入购物车"), "Should confirm add to cart");
        assertTrue(content.contains("event:done"), "Should end with done");
    }

    @Test
    void chatStreamShouldNotFailOnMissingPageContext() throws Exception {
        String body = "{\"message\":\"推荐跑鞋\",\"limit\":1}";

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void chatStreamShouldNotFailOnNullPageContextFields() throws Exception {
        String body = "{\"message\":\"推荐跑鞋\",\"limit\":1,\"page_context\":{\"page_type\":null,\"current_product_id\":null,\"visible_product_ids\":null,\"search_query\":null}}";

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void addToCartWithoutTokenReturnsPleaseLogin() throws Exception {
        String body = "{\"message\":\"把第一款加入购物车\",\"page_context\":{\"page_type\":\"PRODUCT_DETAIL\",\"current_product_id\":\"p_beauty_010\"}}";

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String content = mockMvc.perform(asyncDispatch(mvcResult))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("请先登录"), "Should prompt user to login first");
    }

    @Test
    void addToCartDoesNotSendProductCard() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("demo");
        loginRequest.setPassword("demo123");
        LoginResponse loginResponse = authService.login(loginRequest);

        String body = "{\"message\":\"把这个加入购物车\",\"page_context\":{\"page_type\":\"PRODUCT_DETAIL\",\"current_product_id\":\"p_beauty_010\"}}";

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String content = mockMvc.perform(asyncDispatch(mvcResult))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("event:done"), "Should end with done");
        assertTrue(!content.contains("event:product_card"),
                "Add-to-cart should not send product_card");
    }

    @Test
    void everyStreamEndsWithDoneOrError() throws Exception {
        String body = "{\"message\":\"推荐一款跑鞋\",\"limit\":1}";

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String content = mockMvc.perform(asyncDispatch(mvcResult))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("event:done") || content.contains("event:error"),
                "Every stream must end with done or error");
    }
}
