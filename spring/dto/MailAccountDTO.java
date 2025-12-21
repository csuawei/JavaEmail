package com.db.spring.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;
@Data
public class MailAccountDTO {

    private Long id;

    private String email;

    private String authCode;
}
