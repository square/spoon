/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spoon.ordering.wizard.model;

import android.support.v4.app.Fragment;
import android.text.TextUtils;
import com.example.spoon.ordering.wizard.ui.SingleChoiceFragment;
import java.util.ArrayList;
import java.util.Arrays;

/** A page offering the user a number of mutually exclusive choices. */
public class SingleFixedChoicePage extends Page {
  protected ArrayList<String> mChoices = new ArrayList<String>();

  public SingleFixedChoicePage(ModelCallbacks callbacks, String title) {
    super(callbacks, title);
  }

  @Override
  public Fragment createFragment() {
    return SingleChoiceFragment.create(getKey());
  }

  public String getOptionAt(int position) {
    return mChoices.get(position);
  }

  public int getOptionCount() {
    return mChoices.size();
  }

  @Override public void getReviewItems(ArrayList<ReviewItem> dest) {
    dest.add(new ReviewItem(getTitle(), mData.getString(SIMPLE_DATA_KEY), getKey()));
  }

  @Override public boolean isCompleted() {
    return !TextUtils.isEmpty(mData.getString(SIMPLE_DATA_KEY));
  }

  public SingleFixedChoicePage setChoices(String... choices) {
    mChoices.addAll(Arrays.asList(choices));
    return this;
  }

  public SingleFixedChoicePage setValue(String value) {
    mData.putString(SIMPLE_DATA_KEY, value);
    return this;
  }
}
