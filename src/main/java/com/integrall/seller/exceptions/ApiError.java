package com.integrall.seller.exceptions;


public record ApiError(

        String code,

        String message

) {
}