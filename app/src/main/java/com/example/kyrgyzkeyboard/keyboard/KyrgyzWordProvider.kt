package com.example.kyrgyzkeyboard.keyboard

/**
 * Kyrgyz word candidate provider.
 *
 * The engine stays free of Android dependencies; the Service side supplies an
 * implementation backed by an in-memory index plus Room persistence.
 */
interface KyrgyzWordProvider {

    /** Return candidate words for the given letter prefix, ordered by frequency desc. */
    fun query(prefix: String): List<String>

    /** Record that [word] was committed, for local self-learning (async persistence). */
    fun learn(word: String)
}