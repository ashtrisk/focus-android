/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helpers.TestHelper;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withTitleText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;
import static org.mozilla.focus.helpers.EspressoHelper.openSettings;
import static org.mozilla.focus.helpers.TestHelper.mDevice;
import static org.mozilla.focus.helpers.TestHelper.waitingTime;

@RunWith(AndroidJUnit4.class)
public class URLCompletionTest {
    String site = "680news.com";

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class) {

        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            Context appContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext();
            PreferenceManager.getDefaultSharedPreferences(appContext)
                    .edit()
                    .putBoolean(FIRSTRUN_PREF, true)
                    .apply();
        }
    };

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void CompletionTest() throws InterruptedException, UiObjectNotFoundException {
        /* type a partial url, and check it autocompletes*/
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText("mozilla");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("mozilla.org"));

        /* press x to delete the both autocomplete and suggestion */
        TestHelper.cleartextField.click();
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("Search or enter address"));
        assertFalse (TestHelper.hint.exists());

        /* type a full url, and check it does not autocomplete */
        TestHelper.inlineAutocompleteEditText.setText("http://www.mozilla.org");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("http://www.mozilla.org"));
    }

    @Test
    public void CustomCompletionTest() throws InterruptedException, UiObjectNotFoundException {
        // add custom urls in settings
        // Enable custom autocomplete, and add 2 URLs
        // Open Settings->Autocomplete menu
        OpenCustomCompleteDialog();

        // Enable URL autocomplete, and tap Custom URL add button
        toggleAutocomplete();
        addAutoComplete(site);
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();

        // Check for custom autocompletion
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText("68");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals(site));
        TestHelper.cleartextField.click();

        // Remove custom autocompletion site
        OpenCustomCompleteDialog();
        removeACSite();
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();

        // Check autocompletion
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText("68");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("68"));
        TestHelper.cleartextField.click();

        // add custom autocompletion site, but disable autocomplete
        OpenCustomCompleteDialog();
        addAutoComplete("geckoview.com");
        Espresso.pressBack();
        toggleAutocomplete();       // Disable autocomplete
        Espresso.pressBack();
        Espresso.pressBack();

        // Check autocompletion
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText("gecko");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("gecko"));
        TestHelper.cleartextField.click();

        // Now enable autocomplete
        OpenCustomCompleteDialog();
        toggleAutocomplete();
        Espresso.pressBack();
        Espresso.pressBack();

        // Check autocompletion
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText("gecko");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("geckoview.com"));
        TestHelper.cleartextField.click();

        // Cleanup
        OpenCustomCompleteDialog();
        removeACSite();
        Espresso.pressBack();
        toggleAutocomplete();       // Disable autocomplete
        Espresso.pressBack();
        Espresso.pressBack();
    }

    @Test
    public void DuplicateACSiteTest() throws UiObjectNotFoundException {
        OpenCustomCompleteDialog();

        // Enable URL autocomplete, and tap Custom URL add button
        toggleAutocomplete();
        addAutoComplete(site);
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();

        // Try to add same site again
        OpenCustomCompleteDialog();
        DataInteraction CustomURLRow = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
                .atPosition(4);
        CustomURLRow.perform(click());
        mDevice.waitForIdle();

        onView(withText("+ Add custom URL"))
                .perform(click());
        onView(withId(R.id.domainView))
                .check(matches(isDisplayed()));
        onView(withId(R.id.domainView))
                .perform(typeText(site), closeSoftKeyboard());
        onView(withId(R.id.save))
                .perform(click());

        // Espresso cannot detect the "Already exists" popup.  Instead, check that it's
        // still in the same page
        onView(withId(R.id.domainView))
                .check(matches(isDisplayed()));
        onView(withId(R.id.save))
                .check(matches(isDisplayed()));
        Espresso.pressBack();
        Espresso.pressBack();

        // Cleanup
        removeACSite();
        Espresso.pressBack();
        toggleAutocomplete();       // Disable autocomplete
        Espresso.pressBack();
        Espresso.pressBack();
    }

    private static void toggleAutocomplete() {
        onView(withText("Add and manage custom autocomplete URLs."))
                .perform(click());
    }

    private static void OpenCustomCompleteDialog() {
        mDevice.waitForIdle();
        openSettings();

        onData(withTitleText("URL Autocomplete"))
                .check(matches(isDisplayed()))
                .perform(click());
        mDevice.waitForIdle();
    }

    private static void removeACSite() {
        DataInteraction CustomURLRow = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
                .atPosition(4);
        CustomURLRow.perform(click());
        mDevice.waitForIdle();
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getContext());
        mDevice.waitForIdle();   // wait until dialog fully appears
        onView(withText("Remove"))
                .perform(click());
        onView(withId(R.id.checkbox))
                .perform((click()));
        onView(withId(R.id.remove))
                .perform((click()));
    }

    private static void addAutoComplete(String sitename) {
        DataInteraction CustomURLRow = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
                .atPosition(4);
        CustomURLRow.perform(click());
        mDevice.waitForIdle();

        onView(withText("+ Add custom URL"))
                .perform(click());
        onView(withId(R.id.domainView))
                .check(matches(isDisplayed()));

        onView(withId(R.id.domainView))
                .perform(typeText(sitename), closeSoftKeyboard());
        onView(withId(R.id.save))
                .perform(click());
        onView(withText("Custom URLs"))
                .check(matches(isDisplayed()));

        // Verify new entry appears in the list
        onView(allOf(withText(sitename), withId(R.id.domainView)))
                .check(matches(isDisplayed()));
        mDevice.waitForIdle();

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
