package com.dksd.dvim.telescope;

import de.gesundkrank.fzf4j.models.Result;

public class TeleResult<T> {
    public final Result result;
    public final TeleEntry<T> teleEntry;

    public TeleResult(Result result, TeleEntry<T> teleEntry) {
        this.result = result;
        this.teleEntry = teleEntry;
    }
}
