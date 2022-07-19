package com.example.virtualcards;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;


import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.virtualcards.control.VirtualCardsActivity;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ViewTests {
    @Rule
    public ActivityScenarioRule<VirtualCardsActivity> activityScenarioRule = new ActivityScenarioRule<>(VirtualCardsActivity.class);

    @Before
    public void launchApp(){
        activityScenarioRule.getScenario();
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.virtualcards", appContext.getPackageName());
    }

    @Test
    public void lobbyMenuHostTest(){
        onView(withId(R.id.buttonHost))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.lobbyMenuView))
                .check(matches(isDisplayed()));
        onView(withId(R.id.connectedInfo))
                .check(matches(isDisplayed()));
        onView(withId(R.id.buttonMakeVisible))
                .check(matches(isClickable()));
        onView(withId(R.id.buttonStart))
                .check(matches(isClickable()));

        onView(withId(R.id.clientButtons))
                .check(matches(not(isDisplayed())));

        onView(withId(R.id.buttonClose))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.mainMenuView))
                .check(matches(isDisplayed()));
    }

    @Test
    public void discoveryMenuTest(){
        onView(withId(R.id.buttonJoin))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.discoveryMenuView))
                .check(matches(isDisplayed()));

        onView(withId(R.id.buttonBack))
                .perform(click())
                .check(doesNotExist());

        onView(withId(R.id.mainMenuView))
                .check(matches(isDisplayed()));
    }
}