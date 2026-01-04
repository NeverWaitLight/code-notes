package org.example.xa.commons;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Ordering {
    private String username;
    private Long productId;
    private Integer amount;
    private BigDecimal bill;
}