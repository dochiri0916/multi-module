package com.dochiri.sample.hexagonal;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        statements = {
                "delete from refresh_tokens",
                "delete from users",
                "delete from departments"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class HexagonalUserDepartmentApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 부서와_사용자를_생성하고_조회한다() throws Exception {
        Long departmentId = createDepartment("Platform", "공통 플랫폼을 관리하는 부서");

        MvcResult createUserResult = mockMvc.perform(post("/api/public/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Song",
                                  "email": "song@example.com",
                                  "departmentId": %d
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Song"))
                .andExpect(jsonPath("$.email").value("song@example.com"))
                .andReturn();

        Long userId = readId(createUserResult);

        mockMvc.perform(get("/api/public/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.departmentId").value(departmentId));

        mockMvc.perform(get("/api/public/departments/{departmentId}/users", departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("song@example.com"));
    }

    @Test
    void 존재하지_않는_부서에_사용자를_등록하면_404를_반환한다() throws Exception {
        mockMvc.perform(post("/api/public/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ghost",
                                  "email": "ghost@example.com",
                                  "departmentId": 999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void 동일한_이메일은_중복_등록할_수_없다() throws Exception {
        Long departmentId = createDepartment("People", "인사 부서");

        mockMvc.perform(post("/api/public/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "First",
                                  "email": "duplicate@example.com",
                                  "departmentId": %d
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/public/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Second",
                                  "email": "duplicate@example.com",
                                  "departmentId": %d
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USER_EMAIL"));
    }

    private Long createDepartment(String name, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "%s"
                                }
                                """.formatted(name, description)))
                .andExpect(status().isCreated())
                .andReturn();

        return readId(result);
    }

    private Long readId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
