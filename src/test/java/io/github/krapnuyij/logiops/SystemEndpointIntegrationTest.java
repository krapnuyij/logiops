package io.github.krapnuyij.logiops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemEndpointIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void healthEndpointIsAvailable() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void openApiEndpointIsAvailable() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.paths['/api/v1/inventories/{productId}']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/inventories/{productId}/receipts']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/stock-movements']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders/{orderId}']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders/{orderId}/ship']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders/{orderId}/cancel']").exists())
        .andExpect(jsonPath("$.components.schemas.ApiProblemDetail.properties.errorCode").exists())
        .andExpect(jsonPath("$.components.schemas.ApiProblemDetail.properties.fieldErrors").exists())
        .andExpect(jsonPath("$.components.schemas.ApiProblemDetail.properties.properties").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['409']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['500']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/products'].get.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/products'].get.responses['500']").exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/products/{productId}'].get.responses['404']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/inventories/{productId}'].get.responses['400']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/inventories/{productId}'].get.responses['404']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/inventories/{productId}/receipts'].post.responses['500']"
        ).exists())
        .andExpect(jsonPath("$.paths['/api/v1/stock-movements'].get.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/stock-movements'].get.responses['500']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders'].post.responses['400']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders'].post.responses['404']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders'].post.responses['409']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/outbound-orders'].post.responses['500']").exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/outbound-orders/{orderId}'].get.responses['404']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/outbound-orders/{orderId}/ship'].post.responses['400']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/outbound-orders/{orderId}/ship'].post.responses['404']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/outbound-orders/{orderId}/ship'].post.responses['409']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/outbound-orders/{orderId}/ship'].post.responses['500']"
        ).exists())
        .andExpect(jsonPath(
            "$.paths['/api/v1/outbound-orders/{orderId}/cancel'].post.responses['409']"
        ).exists());
  }

  @Test
  void swaggerUiIsAvailable() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }
}
