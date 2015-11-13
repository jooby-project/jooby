package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class NgAnnotateTest {


  @Test
  public void add() throws Exception {
    assertEquals(
        "angular.module(\"MyMod\").controller(\"MyCtrl\", [\"$scope\", \"$timeout\", function($scope, $timeout) {\n"
            +
            "}]);",
        new NgAnnotate().process("/x.js",
            "angular.module(\"MyMod\").controller(\"MyCtrl\", function($scope, $timeout) {\n" +
                "});",
                ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void error() throws Exception {
    assertEquals(
        "angular.module(\"MyMod\").controller(\"MyCtrl\", [\"$scope\", \"$timeout\", function($scope, $timeout) {\n"
            +
            "}]);",
        new NgAnnotate().process("/x.js",
            "angular.module(\"MyMod\").controller(\"MyCtrl\", function($scope $timeout) {\n" +
                "});",
                ConfigFactory.empty()));
  }

}
