package com.capstone25.WildFirePrediction.global.exception.handler;

import com.capstone25.WildFirePrediction.global.code.BaseErrorCode;
import com.capstone25.WildFirePrediction.global.exception.GeneralException;

public class ExceptionHandler extends GeneralException {
    public ExceptionHandler(BaseErrorCode code) {
        super(code);
    }
}
