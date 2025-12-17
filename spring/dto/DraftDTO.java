package com.db.spring.dto;


import lombok.Data;

@Data
public class DraftDTO {
    private String senderEmail;

    private String receiverEmail;

    private String  subject;

    private String  content;
}
