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

import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a wizard model, including the pages/steps in the wizard, their dependencies, and
 * their
 * currently populated choices/values/selections.
 *
 * To create an actual wizard model, extend this class and implement {@link #onNewRootPageList()}.
 */
public abstract class AbstractWizardModel implements ModelCallbacks {
  protected Context mContext;

  private List<ModelCallbacks> mListeners = new ArrayList<ModelCallbacks>();
  private PageList mRootPageList;

  public AbstractWizardModel(Context context) {
    mRootPageList = onNewRootPageList();
    mContext = context;
  }

  /** Override this to define a new wizard model. */
  protected abstract PageList onNewRootPageList();

  @Override public void onPageDataChanged(Page page) {
    // can't use for each because of concurrent modification (review fragment
    // can get added or removed and will register itself as a listener)
    for (int i = 0; i < mListeners.size(); i++) {
      mListeners.get(i).onPageDataChanged(page);
    }
  }

  @Override public void onPageTreeChanged() {
    // can't use for each because of concurrent modification (review fragment
    // can get added or removed and will register itself as a listener)
    for (int i = 0; i < mListeners.size(); i++) {
      mListeners.get(i).onPageTreeChanged();
    }
  }

  public Page findByKey(String key) {
    return mRootPageList.findByKey(key);
  }

  public void load(Bundle savedValues) {
    for (String key : savedValues.keySet()) {
      mRootPageList.findByKey(key).resetData(savedValues.getBundle(key));
    }
  }

  public void registerListener(ModelCallbacks listener) {
    mListeners.add(listener);
  }

  public Bundle save() {
    Bundle bundle = new Bundle();
    for (Page page : getCurrentPageSequence()) {
      bundle.putBundle(page.getKey(), page.getData());
    }
    return bundle;
  }

  /**
   * Gets the current list of wizard steps, flattening nested (dependent) pages based on the
   * user's choices.
   */
  public List<Page> getCurrentPageSequence() {
    ArrayList<Page> flattened = new ArrayList<Page>();
    mRootPageList.flattenCurrentPageSequence(flattened);
    return flattened;
  }

  public void unregisterListener(ModelCallbacks listener) {
    mListeners.remove(listener);
  }
}
