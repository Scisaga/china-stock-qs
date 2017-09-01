package org.tfelab.io.requester;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * Licensed under MIT License
 * Wraps a condition so that it returns only after the document state has settled for a given time, the default being 2 seconds. The
 * document is considered settled when the "document.readyState" field stays "complete" and the URL in the browser stops changing.
 */
public class DocumentSettleCondition<T> implements ExpectedCondition<T> {
	
    private final ExpectedCondition<T> condition;
    private final long settleTimeInMillis;

    private long lastComplete = 0L;
    private String lastUrl;

    public DocumentSettleCondition(ExpectedCondition<T> condition, long settleTimeInMillis) {
        this.condition = condition;
        this.settleTimeInMillis = settleTimeInMillis;
    }

    public DocumentSettleCondition(ExpectedCondition<T> condition) {
        this(condition, 1000L);
    }

    /**
     * Get the settle time in millis.
     */
    public long getSettleTime() {
        return settleTimeInMillis;
    }

    @Override
    public T apply(WebDriver driver) {
        if (driver instanceof JavascriptExecutor) {
            String currentUrl = driver.getCurrentUrl();
            String readyState = String.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"));
            boolean complete = readyState.equalsIgnoreCase("complete");
            if (!complete) {
                lastComplete = 0L;
                return null;
            }

            if (lastUrl != null && !lastUrl.equals(currentUrl)) {
                lastComplete = 0L;
            }
            lastUrl = currentUrl;

            if (lastComplete == 0L) {
                lastComplete = System.currentTimeMillis();
                return null;
            }
            long settleTime = System.currentTimeMillis() - lastComplete;
            if (settleTime < this.settleTimeInMillis) {
                return null;
            }
        }
        return condition.apply(driver);
    }

    @Override
    public String toString() {
        return "Document settle @" + settleTimeInMillis + "ms for " + condition;
    }
}