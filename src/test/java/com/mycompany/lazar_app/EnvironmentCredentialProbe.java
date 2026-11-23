package com.mycompany.lazar_app;
/** Test-only child: checks credential source without printing its value. */
public final class EnvironmentCredentialProbe {
 public static void main(String[]args){
  String expected=System.getenv("GYMFLOW_DB_PASSWORD");
  if(expected==null||!expected.equals(com.mycompany.lazar_app.database.DatabaseConfigManager.load().getPassword()))System.exit(1);
  System.out.println("Environment credential used without serialization or disclosure");
 }
}
