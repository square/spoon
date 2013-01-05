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

package com.squareup.spoon.sample.wizard.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.squareup.spoon.sample.R;
import com.squareup.spoon.sample.wizard.model.MultipleFixedChoicePage;
import com.squareup.spoon.sample.wizard.model.Page;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultipleChoiceFragment extends ListFragment {
  private static final String ARG_KEY = "key";

  private PageFragmentCallbacks mCallbacks;
  private String mKey;
  private List<String> mChoices;
  private Page mPage;

  public static MultipleChoiceFragment create(String key) {
    Bundle args = new Bundle();
    args.putString(ARG_KEY, key);

    MultipleChoiceFragment fragment = new MultipleChoiceFragment();
    fragment.setArguments(args);
    return fragment;
  }

  public MultipleChoiceFragment() {
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    mKey = args.getString(ARG_KEY);
    mPage = mCallbacks.onGetPage(mKey);

    MultipleFixedChoicePage fixedChoicePage = (MultipleFixedChoicePage) mPage;
    mChoices = new ArrayList<String>();
    for (int i = 0; i < fixedChoicePage.getOptionCount(); i++) {
      mChoices.add(fixedChoicePage.getOptionAt(i));
    }
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_page, container, false);
    ((TextView) rootView.findViewById(android.R.id.title)).setText(mPage.getTitle());

    final ListView listView = (ListView) rootView.findViewById(android.R.id.list);
    setListAdapter(
        new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice,
            android.R.id.text1, mChoices));
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    // Pre-select currently selected items.
    new Handler().post(new Runnable() {
      @Override
      public void run() {
        ArrayList<String> selectedItems = mPage.getData().getStringArrayList(Page.SIMPLE_DATA_KEY);
        if (selectedItems == null || selectedItems.size() == 0) {
          return;
        }

        Set<String> selectedSet = new HashSet<String>(selectedItems);

        for (int i = 0; i < mChoices.size(); i++) {
          if (selectedSet.contains(mChoices.get(i))) {
            listView.setItemChecked(i, true);
          }
        }
      }
    });

    return rootView;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (!(activity instanceof PageFragmentCallbacks)) {
      throw new ClassCastException("Activity must implement PageFragmentCallbacks");
    }

    mCallbacks = (PageFragmentCallbacks) activity;
  }

  @Override public void onDetach() {
    super.onDetach();
    mCallbacks = null;
  }

  @Override public void onListItemClick(ListView l, View v, int position, long id) {
    SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();
    ArrayList<String> selections = new ArrayList<String>();
    for (int i = 0; i < checkedPositions.size(); i++) {
      if (checkedPositions.valueAt(i)) {
        selections.add(getListAdapter().getItem(checkedPositions.keyAt(i)).toString());
      }
    }

    mPage.getData().putStringArrayList(Page.SIMPLE_DATA_KEY, selections);
    mPage.notifyDataChanged();
  }
}
