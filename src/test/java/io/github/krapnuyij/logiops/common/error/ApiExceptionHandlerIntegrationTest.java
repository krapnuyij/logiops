package io.github.krapnuyij.logiops.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiExceptionHandlerIntegrationTest.FailingController.class)
class ApiExceptionHandlerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void hidesInternalExceptionAndSqlDetails() throws Exception {
    String responseBody = mockMvc.perform(get("/test/unexpected-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Internal Server Error"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value("예상하지 못한 서버 오류가 발생했다."))
        .andExpect(jsonPath("$.instance").value("/test/unexpected-error"))
        .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.fieldErrors").doesNotExist())
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(responseBody)
        .doesNotContain("inventory_secret_constraint", "DataIntegrityViolationException");
  }

  @Test
  void preservesFrameworkStatusForUnknownPathAndUnsupportedMethod() throws Exception {
    mockMvc.perform(get("/unknown-path"))
        .andExpect(status().isNotFound());
    mockMvc.perform(post("/test/unexpected-error"))
        .andExpect(status().isMethodNotAllowed());
  }

  @RestController
  static class FailingController {

    @GetMapping("/test/unexpected-error")
    void fail() {
      throw new DataIntegrityViolationException("inventory_secret_constraint");
    }
  }
}
