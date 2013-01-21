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

package com.example.spoon.ordering;

import android.content.Context;
import com.example.spoon.ordering.wizard.model.AbstractWizardModel;
import com.example.spoon.ordering.wizard.model.BranchPage;
import com.example.spoon.ordering.wizard.model.CustomerInfoPage;
import com.example.spoon.ordering.wizard.model.MultipleFixedChoicePage;
import com.example.spoon.ordering.wizard.model.PageList;
import com.example.spoon.ordering.wizard.model.SingleFixedChoicePage;

public class SandwichWizardModel extends AbstractWizardModel {
  public SandwichWizardModel(Context context) {
    super(context);
  }

  @Override protected PageList onNewRootPageList() {
    return new PageList(//
        new BranchPage(this, "Order type") //
            .addBranch("Sandwich", //
                new SingleFixedChoicePage(this, "Bread") //
                    .setChoices("White", "Wheat", "Rye", "Pretzel", "Ciabatta") //
                    .setRequired(true), //

                new MultipleFixedChoicePage(this, "Meats") //
                    .setChoices("Pepperoni", "Turkey", "Ham", "Pastrami", "Roast Beef", "Bologna"),

                new MultipleFixedChoicePage(this, "Veggies") //
                    .setChoices("Tomatoes", "Lettuce", "Onions", "Pickles", "Cucumbers", "Peppers"),

                new MultipleFixedChoicePage(this, "Cheeses") //
                    .setChoices("Swiss", "American", "Pepperjack", "Muenster", "Provolone",
                        "White American", "Cheddar", "Bleu"),

                new BranchPage(this, "Toasted?") //
                    .addBranch("Yes", new SingleFixedChoicePage(this, "Toast time") //
                        .setChoices("30 seconds", "1 minute", "2 minutes"))
                    .addBranch("No")
                    .setValue("No"))

            .addBranch("Salad", //
                new SingleFixedChoicePage(this, "Salad type") //
                    .setChoices("Greek", "Caesar") //
                    .setRequired(true),

                new SingleFixedChoicePage(this, "Dressing") //
                    .setChoices("No dressing", "Balsamic", "Oil & vinegar", "Thousand Island",
                        "Italian") //
                    .setValue("No dressing"))

            .setRequired(true),

        new CustomerInfoPage(this, "Your info") //
            .setRequired(true));
  }
}
