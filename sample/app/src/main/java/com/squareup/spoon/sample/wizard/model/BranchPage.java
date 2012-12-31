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

package com.squareup.spoon.sample.wizard.model;

import android.support.v4.app.Fragment;
import android.text.TextUtils;
import com.squareup.spoon.sample.wizard.ui.SingleChoiceFragment;
import java.util.ArrayList;
import java.util.List;

/**
 * A page representing a branching point in the wizard. Depending on which choice is selected, the
 * next set of steps in the wizard may change.
 */
public class BranchPage extends SingleFixedChoicePage {
  private List<Branch> mBranches = new ArrayList<Branch>();

  public BranchPage(ModelCallbacks callbacks, String title) {
    super(callbacks, title);
  }

  @Override public Page findByKey(String key) {
    if (getKey().equals(key)) {
      return this;
    }

    for (Branch branch : mBranches) {
      Page found = branch.childPageList.findByKey(key);
      if (found != null) {
        return found;
      }
    }

    return null;
  }

  @Override public void flattenCurrentPageSequence(ArrayList<Page> destination) {
    super.flattenCurrentPageSequence(destination);
    for (Branch branch : mBranches) {
      if (branch.choice.equals(mData.getString(Page.SIMPLE_DATA_KEY))) {
        branch.childPageList.flattenCurrentPageSequence(destination);
        break;
      }
    }
  }

  public BranchPage addBranch(String choice, Page... childPages) {
    PageList childPageList = new PageList(childPages);
    for (Page page : childPageList) {
      page.setParentKey(choice);
    }
    mBranches.add(new Branch(choice, childPageList));
    return this;
  }

  @Override public Fragment createFragment() {
    return SingleChoiceFragment.create(getKey());
  }

  public String getOptionAt(int position) {
    return mBranches.get(position).choice;
  }

  public int getOptionCount() {
    return mBranches.size();
  }

  @Override public void getReviewItems(ArrayList<ReviewItem> dest) {
    dest.add(new ReviewItem(getTitle(), mData.getString(SIMPLE_DATA_KEY), getKey()));
  }

  @Override public boolean isCompleted() {
    return !TextUtils.isEmpty(mData.getString(SIMPLE_DATA_KEY));
  }

  @Override public void notifyDataChanged() {
    mCallbacks.onPageTreeChanged();
    super.notifyDataChanged();
  }

  public BranchPage setValue(String value) {
    mData.putString(SIMPLE_DATA_KEY, value);
    return this;
  }

  private static class Branch {
    public String choice;
    public PageList childPageList;

    private Branch(String choice, PageList childPageList) {
      this.choice = choice;
      this.childPageList = childPageList;
    }
  }
}
