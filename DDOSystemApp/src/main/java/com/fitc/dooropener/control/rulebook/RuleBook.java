package com.fitc.dooropener.control.rulebook;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jon on 22/02/2016.
 */
public class RuleBook {

    List<Rule> mRules = new ArrayList<>();

    public void addRule(Rule rule){
        mRules.add(rule);
    }

    public boolean isObeyed(){
        for (Rule r:mRules){
            if (!r.isObeyed()) return false;
        }
        return true;
    }




}
