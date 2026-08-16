package org.spongepowered.include.com.google.gson;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.include.com.google.gson.internal.Excluder;
import org.spongepowered.include.com.google.gson.reflect.TypeToken;

public final class GsonBuilder {
   private Excluder excluder;
   private LongSerializationPolicy longSerializationPolicy;
   private FieldNamingStrategy fieldNamingPolicy;
   private final Map<Type, InstanceCreator<?>> instanceCreators;
   private final List<TypeAdapterFactory> factories;
   private final List<TypeAdapterFactory> hierarchyFactories;
   private boolean serializeNulls;
   private String datePattern;
   private int dateStyle;
   private int timeStyle;
   private boolean complexMapKeySerialization;
   private boolean serializeSpecialFloatingPointValues;
   private boolean escapeHtmlChars;
   private boolean prettyPrinting;
   private boolean generateNonExecutableJson;

   public GsonBuilder() {
      this.excluder = Excluder.DEFAULT;
      this.longSerializationPolicy = LongSerializationPolicy.DEFAULT;
      this.fieldNamingPolicy = FieldNamingPolicy.IDENTITY;
      this.instanceCreators = new HashMap();
      this.factories = new ArrayList();
      this.hierarchyFactories = new ArrayList();
      this.dateStyle = 2;
      this.timeStyle = 2;
      this.escapeHtmlChars = true;
   }

   public GsonBuilder setPrettyPrinting() {
      this.prettyPrinting = true;
      return this;
   }

   public GsonBuilder disableHtmlEscaping() {
      this.escapeHtmlChars = false;
      return this;
   }

   public Gson create() {
      List<TypeAdapterFactory> factories = new ArrayList();
      factories.addAll(this.factories);
      Collections.reverse(factories);
      factories.addAll(this.hierarchyFactories);
      this.addTypeAdaptersForDate(this.datePattern, this.dateStyle, this.timeStyle, factories);
      return new Gson(this.excluder, this.fieldNamingPolicy, this.instanceCreators, this.serializeNulls, this.complexMapKeySerialization, this.generateNonExecutableJson, this.escapeHtmlChars, this.prettyPrinting, this.serializeSpecialFloatingPointValues, this.longSerializationPolicy, factories);
   }

   private void addTypeAdaptersForDate(String datePattern, int dateStyle, int timeStyle, List<TypeAdapterFactory> factories) {
      DefaultDateTypeAdapter dateTypeAdapter;
      if (datePattern != null && !"".equals(datePattern.trim())) {
         dateTypeAdapter = new DefaultDateTypeAdapter(datePattern);
      } else {
         if (dateStyle == 2 || timeStyle == 2) {
            return;
         }

         dateTypeAdapter = new DefaultDateTypeAdapter(dateStyle, timeStyle);
      }

      factories.add(TreeTypeAdapter.newFactory(TypeToken.get(Date.class), dateTypeAdapter));
      factories.add(TreeTypeAdapter.newFactory(TypeToken.get(Timestamp.class), dateTypeAdapter));
      factories.add(TreeTypeAdapter.newFactory(TypeToken.get(java.sql.Date.class), dateTypeAdapter));
   }
}
