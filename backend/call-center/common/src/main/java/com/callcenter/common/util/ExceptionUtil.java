package com.callcenter.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常处理工具类。
 */
public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    /**
     * 将异常堆栈转换为字符串，便于日志记录。
     *
     * @param throwable throwable
     * @return stack trace string
     */
    public static String stackTraceToString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
            return stringWriter.toString();
        }
    }
}