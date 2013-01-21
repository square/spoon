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

package com.example.spoon.ordering.wizard.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.example.spoon.ordering.R;
import com.example.spoon.ordering.wizard.model.CustomerInfoPage;

public class CustomerInfoFragment extends Fragment {
  private static final String ARG_KEY = "key";

  private PageFragmentCallbacks mCallbacks;
  private String mKey;
  private CustomerInfoPage mPage;
  private TextView mNameView;
  private TextView mEmailView;

  public static CustomerInfoFragment create(String key) {
    Bundle args = new Bundle();
    args.putString(ARG_KEY, key);

    CustomerInfoFragment fragment = new CustomerInfoFragment();
    fragment.setArguments(args);
    return fragment;
  }

  public CustomerInfoFragment() {
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    mKey = args.getString(ARG_KEY);
    mPage = (CustomerInfoPage) mCallbacks.onGetPage(mKey);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_page_customer_info, container, false);
    ((TextView) rootView.findViewById(android.R.id.title)).setText(mPage.getTitle());

    mNameView = ((TextView) rootView.findViewById(R.id.your_name));
    mNameView.setText(mPage.getData().getString(CustomerInfoPage.NAME_DATA_KEY));

    mEmailView = ((TextView) rootView.findViewById(R.id.your_email));
    mEmailView.setText(mPage.getData().getString(CustomerInfoPage.EMAIL_DATA_KEY));
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

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mNameView.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override public void afterTextChanged(Editable editable) {
        mPage.getData()
            .putString(CustomerInfoPage.NAME_DATA_KEY,
                (editable != null) ? editable.toString() : null);
        mPage.notifyDataChanged();
      }
    });

    mEmailView.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override public void afterTextChanged(Editable editable) {
        mPage.getData()
            .putString(CustomerInfoPage.EMAIL_DATA_KEY,
                (editable != null) ? editable.toString() : null);
        mPage.notifyDataChanged();
      }
    });
  }

  @Override public void setMenuVisibility(boolean menuVisible) {
    super.setMenuVisibility(menuVisible);

    // In a future update to the support library, this should override setUserVisibleHint
    // instead of setMenuVisibility.
    if (mNameView != null) {
      InputMethodManager imm =
          (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (!menuVisible) {
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
      }
    }
  }
}
