package org.spongepowered.asm.util.perf;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public final class Profiler {
   public static final int ROOT = 1;
   public static final int FINE = 2;
   private static final Map<String, Profiler> profilers = new HashMap();
   private static boolean active;
   private final String id;
   private final Map<String, Section> sections = new TreeMap();
   private final List<String> phases = new ArrayList();
   private final Deque<Section> stack = new LinkedList();

   public Profiler(String id) {
      this.id = id;
      this.phases.add("Initial");
   }

   public String toString() {
      return this.id;
   }

   public static void setActive(boolean active) {
      Profiler.active = active;
   }

   public synchronized void reset() {
      for(Section section : this.sections.values()) {
         section.invalidate();
      }

      this.sections.clear();
      this.phases.clear();
      this.phases.add("Initial");
      this.stack.clear();
   }

   public synchronized Section get(String name) {
      Section section = (Section)this.sections.get(name);
      if (section == null) {
         section = (Section)(active ? new LiveSection(name, this.phases.size() - 1) : new DisabledSection(name));
         this.sections.put(name, section);
      }

      return section;
   }

   private synchronized Section getSubSection(String name, String baseName, Section root) {
      Section section = (Section)this.sections.get(name);
      if (section == null) {
         section = new SubSection(name, this.phases.size() - 1, baseName, root);
         this.sections.put(name, section);
      }

      return section;
   }

   public Section begin(String... path) {
      return this.begin(0, (String[])path);
   }

   public Section begin(int flags, String... path) {
      return this.begin(flags, Joiner.on('.').join(path));
   }

   public Section begin(String name) {
      return this.begin(0, (String)name);
   }

   public synchronized Section begin(int flags, String name) {
      boolean root = (flags & 1) != 0;
      boolean fine = (flags & 2) != 0;
      String path = name;
      Section head = (Section)this.stack.peek();
      if (head != null) {
         path = head.getName() + (root ? " -> " : ".") + name;
         if (head.isRoot() && !root) {
            int pos = head.getName().lastIndexOf(" -> ");
            name = (pos > -1 ? head.getName().substring(pos + 4) : head.getName()) + "." + name;
            root = true;
         }
      }

      Section section = this.get(root ? name : path);
      if (root && head != null && active) {
         section = this.getSubSection(path, head.getName(), section);
      }

      section.setFine(fine).setRoot(root);
      this.stack.push(section);
      return section.start();
   }

   synchronized void end(Section section) {
      try {
         Section head = (Section)this.stack.pop();

         for(Section next = head; next != section; next = (Section)this.stack.pop()) {
            if (next == null && active) {
               if (head == null) {
                  throw new IllegalStateException("Attempted to pop " + section + " but the stack is empty");
               }

               throw new IllegalStateException("Attempted to pop " + section + " which was not in the stack, head was " + head);
            }
         }
      } catch (NoSuchElementException var4) {
         if (active) {
            throw new IllegalStateException("Attempted to pop " + section + " but the stack is empty");
         }
      }

   }

   public synchronized void mark(String phase) {
      long currentPhaseTime = 0L;

      for(Section section : this.sections.values()) {
         currentPhaseTime += section.getTime();
      }

      if (currentPhaseTime == 0L) {
         int size = this.phases.size();
         this.phases.set(size - 1, phase);
      } else {
         this.phases.add(phase);

         for(Section section : this.sections.values()) {
            section.mark();
         }

      }
   }

   public synchronized Collection<Section> getSections() {
      return Collections.unmodifiableCollection(this.sections.values());
   }

   public PrettyPrinter printer(boolean includeFine, boolean group) {
      return printer(includeFine, group, this.phases, this.sections);
   }

   private static PrettyPrinter printer(boolean includeFine, boolean group, List<String> phases, Map<String, Section> sections) {
      PrettyPrinter printer = new PrettyPrinter();
      int colCount = phases.size() + 4;
      int[] columns = new int[]{0, 1, 2, colCount - 2, colCount - 1};
      Object[] headers = new Object[colCount * 2];
      int col = 0;

      for(int pos = 0; col < colCount; pos = col * 2) {
         headers[pos + 1] = PrettyPrinter.Alignment.RIGHT;
         if (col == columns[0]) {
            headers[pos] = (group ? "" : "  ") + "Section";
            headers[pos + 1] = PrettyPrinter.Alignment.LEFT;
         } else if (col == columns[1]) {
            headers[pos] = "    TOTAL";
         } else if (col == columns[3]) {
            headers[pos] = "    Count";
         } else if (col == columns[4]) {
            headers[pos] = "Avg. ";
         } else if (col - columns[2] < phases.size()) {
            headers[pos] = phases.get(col - columns[2]);
         } else {
            headers[pos] = "";
         }

         ++col;
      }

      printer.table(headers).th().hr().add();

      for(Section section : sections.values()) {
         if ((!section.isFine() || includeFine) && (!group || section.getDelegate() == section)) {
            printSectionRow(printer, colCount, columns, section, group);
            if (group) {
               for(Section subSection : sections.values()) {
                  Section delegate = subSection.getDelegate();
                  if ((!subSection.isFine() || includeFine) && delegate == section && delegate != subSection) {
                     printSectionRow(printer, colCount, columns, subSection, group);
                  }
               }
            }
         }
      }

      return printer.add();
   }

   private static void printSectionRow(PrettyPrinter printer, int colCount, int[] columns, Section section, boolean group) {
      boolean isDelegate = section.getDelegate() != section;
      Object[] values = new Object[colCount];
      int col = 1;
      if (group) {
         values[0] = isDelegate ? "  > " + section.getBaseName() : section.getName();
      } else {
         values[0] = (isDelegate ? "+ " : "  ") + section.getName();
      }

      long[] times = section.getTimes();

      for(long time : times) {
         if (col == columns[1]) {
            values[col++] = section.getTotalTime() + " ms";
         }

         if (col >= columns[2] && col < values.length) {
            values[col++] = time + " ms";
         }
      }

      values[columns[3]] = section.getTotalCount();
      values[columns[4]] = (new DecimalFormat("   ###0.000 ms")).format(section.getTotalAverageTime());

      for(int i = 0; i < values.length; ++i) {
         if (values[i] == null) {
            values[i] = "-";
         }
      }

      printer.tr(values);
   }

   public void printSummary() {
      printSummary(this.id, this.phases, this.sections);
   }

   public static void printAuditSummary() {
      String id;
      Set<String> allPhases;
      Map<String, Section> allSections;
      synchronized(profilers) {
         id = Joiner.on(',').join(profilers.values());
         allPhases = new LinkedHashSet();
         allSections = new TreeMap<String, Section>() {
            public Section get(Object name) {
               Section section = (Section)super.get(name);
               if (section == null) {
                  this.put(name.toString(), section = new ResultSection(name.toString()));
               }

               return section;
            }
         };

         for(Profiler profiler : profilers.values()) {
            for(String phase : profiler.phases) {
               allPhases.add(phase);
            }

            for(Map.Entry<String, Section> section : profiler.sections.entrySet()) {
               ((ResultSection)allSections.get(section.getKey())).add((Section)section.getValue());
            }
         }
      }

      printSummary(id, new ArrayList(allPhases), allSections);
   }

   private static void printSummary(String id, List<String> phases, Map<String, Section> sections) {
      DecimalFormat threedp = new DecimalFormat("(###0.000");
      DecimalFormat onedp = new DecimalFormat("(###0.0");
      PrettyPrinter printer = printer(false, false, phases, sections);
      long prepareTime = ((Section)sections.get("mixin.prepare")).getTotalTime();
      long readTime = ((Section)sections.get("mixin.read")).getTotalTime();
      long applyTime = ((Section)sections.get("mixin.apply")).getTotalTime();
      long writeTime = ((Section)sections.get("mixin.write")).getTotalTime();
      long totalMixinTime = ((Section)sections.get("mixin")).getTotalTime();
      long loadTime = ((Section)sections.get("class.load")).getTotalTime();
      long transformTime = ((Section)sections.get("class.transform")).getTotalTime();
      long exportTime = ((Section)sections.get("mixin.debug.export")).getTotalTime();
      long actualTime = totalMixinTime - loadTime - transformTime - exportTime;
      double timeSliceMixin = (double)actualTime / (double)totalMixinTime * (double)100.0F;
      double timeSliceLoad = (double)loadTime / (double)totalMixinTime * (double)100.0F;
      double timeSliceTransform = (double)transformTime / (double)totalMixinTime * (double)100.0F;
      double timeSliceExport = (double)exportTime / (double)totalMixinTime * (double)100.0F;
      long worstTransformerTime = 0L;
      Section worstTransformer = null;

      for(Section section : sections.values()) {
         long transformerTime = section.getName().startsWith("class.transform.") ? section.getTotalTime() : 0L;
         if (transformerTime > worstTransformerTime) {
            worstTransformerTime = transformerTime;
            worstTransformer = section;
         }
      }

      printer.hr().add("Summary for Profiler[%s]", id).hr().add();
      String format = "%9d ms %12s seconds)";
      printer.kv("Total mixin time", format, totalMixinTime, threedp.format((double)totalMixinTime * 0.001)).add();
      printer.kv("Preparing mixins", format, prepareTime, threedp.format((double)prepareTime * 0.001));
      printer.kv("Reading input", format, readTime, threedp.format((double)readTime * 0.001));
      printer.kv("Applying mixins", format, applyTime, threedp.format((double)applyTime * 0.001));
      printer.kv("Writing output", format, writeTime, threedp.format((double)writeTime * 0.001)).add();
      printer.kv("of which", "");
      printer.kv("Time spent loading from disk", format, loadTime, threedp.format((double)loadTime * 0.001));
      printer.kv("Time spent transforming classes", format, transformTime, threedp.format((double)transformTime * 0.001)).add();
      if (worstTransformer != null) {
         printer.kv("Worst transformer", worstTransformer.getName());
         printer.kv("Class", worstTransformer.getInfo());
         printer.kv("Time spent", "%s seconds", worstTransformer.getTotalSeconds());
         printer.kv("called", "%d times", worstTransformer.getTotalCount()).add();
      }

      printer.kv("   Time allocation:     Processing mixins", "%9d ms %10s%% of total)", actualTime, onedp.format(timeSliceMixin));
      printer.kv("Loading classes", "%9d ms %10s%% of total)", loadTime, onedp.format(timeSliceLoad));
      printer.kv("Running transformers", "%9d ms %10s%% of total)", transformTime, onedp.format(timeSliceTransform));
      if (exportTime > 0L) {
         printer.kv("Exporting classes (debug)", "%9d ms %10s%% of total)", exportTime, onedp.format(timeSliceExport));
      }

      printer.add();

      try {
         Class<?> agent = MixinService.getService().getClassProvider().findAgentClass("org.spongepowered.metronome.Agent", false);
         Method mdGetTimes = agent.getDeclaredMethod("getTimes");
         Map<String, Long> times = (Map)mdGetTimes.invoke((Object)null);
         printer.hr().add("Transformer Times").hr().add();
         int longest = 10;

         for(Map.Entry<String, Long> entry : times.entrySet()) {
            longest = Math.max(longest, ((String)entry.getKey()).length());
         }

         for(Map.Entry<String, Long> entry : times.entrySet()) {
            String name = (String)entry.getKey();
            long mixinTime = 0L;

            for(Section section : sections.values()) {
               if (name.equals(section.getInfo())) {
                  mixinTime = section.getTotalTime();
                  break;
               }
            }

            if (mixinTime > 0L) {
               printer.add("%-" + longest + "s %8s ms %8s ms in mixin)", name, (Long)entry.getValue() + mixinTime, "(" + mixinTime);
            } else {
               printer.add("%-" + longest + "s %8s ms", name, entry.getValue());
            }
         }

         printer.add();
      } catch (Throwable var47) {
      }

      printer.print();
   }

   public static Profiler getProfiler(String id) {
      synchronized(profilers) {
         Profiler profiler = (Profiler)profilers.get(id);
         if (profiler == null) {
            profilers.put(id, profiler = new Profiler(id));
         }

         return profiler;
      }
   }

   public static Collection<Profiler> getProfilers() {
      ImmutableList.Builder<Profiler> list = ImmutableList.<Profiler>builder();
      synchronized(profilers) {
         list.addAll(profilers.values());
      }

      return list.build();
   }

   public abstract static class Section {
      private final String name;
      private boolean root;
      private boolean fine;
      protected boolean invalidated;
      private String info;

      Section(String name) {
         this.name = name;
         this.info = name;
      }

      protected int getCursor() {
         return 0;
      }

      Section getDelegate() {
         return this;
      }

      Section invalidate() {
         this.invalidated = true;
         return this;
      }

      Section setRoot(boolean root) {
         this.root = root;
         return this;
      }

      public boolean isRoot() {
         return this.root;
      }

      Section setFine(boolean fine) {
         this.fine = fine;
         return this;
      }

      public boolean isFine() {
         return this.fine;
      }

      public String getName() {
         return this.name;
      }

      public String getBaseName() {
         return this.name;
      }

      public void setInfo(String info) {
         this.info = info;
      }

      public String getInfo() {
         return this.info;
      }

      Section start() {
         return this;
      }

      protected Section stop() {
         return this;
      }

      public Section end() {
         return this;
      }

      public Section next(String name) {
         this.end();
         return this;
      }

      void mark() {
      }

      public long getTime() {
         return 0L;
      }

      public long getTotalTime() {
         return 0L;
      }

      public double getSeconds() {
         return (double)0.0F;
      }

      public double getTotalSeconds() {
         return (double)0.0F;
      }

      public long[] getTimes() {
         return new long[1];
      }

      public int getCount() {
         return 0;
      }

      public int getTotalCount() {
         return 0;
      }

      public double getAverageTime() {
         return (double)0.0F;
      }

      public double getTotalAverageTime() {
         return (double)0.0F;
      }

      public final String toString() {
         return this.name;
      }

      protected long getMarkedTime() {
         return 0L;
      }

      protected int getMarkedCount() {
         return 0;
      }
   }

   class DisabledSection extends Section {
      DisabledSection(String name) {
         super(name);
      }

      public Section end() {
         if (!this.invalidated) {
            Profiler.this.end(this);
         }

         return this;
      }

      public Section next(String name) {
         this.end();
         return Profiler.this.begin(name);
      }
   }

   class LiveSection extends DisabledSection {
      private int cursor = 0;
      private long[] times = new long[0];
      private long start = 0L;
      private long time;
      private long markedTime;
      private int count;
      private int markedCount;

      LiveSection(String name, int cursor) {
         super(name);
         this.cursor = cursor;
      }

      protected int getCursor() {
         return this.cursor;
      }

      Section start() {
         this.start = System.currentTimeMillis();
         return this;
      }

      protected Section stop() {
         if (this.start > 0L) {
            this.time += System.currentTimeMillis() - this.start;
         }

         this.start = 0L;
         ++this.count;
         return this;
      }

      public Section end() {
         this.stop();
         if (!this.invalidated) {
            Profiler.this.end(this);
         }

         return this;
      }

      void mark() {
         if (this.cursor >= this.times.length) {
            this.times = Arrays.copyOf(this.times, this.cursor + 4);
         }

         this.times[this.cursor] = this.time;
         this.markedTime += this.time;
         this.markedCount += this.count;
         this.time = 0L;
         this.count = 0;
         ++this.cursor;
      }

      public long getTime() {
         return this.time;
      }

      public long getTotalTime() {
         return this.time + this.markedTime;
      }

      public double getSeconds() {
         return (double)this.time * 0.001;
      }

      public double getTotalSeconds() {
         return (double)(this.time + this.markedTime) * 0.001;
      }

      public long[] getTimes() {
         long[] times = new long[this.cursor + 1];
         System.arraycopy(this.times, 0, times, 0, Math.min(this.times.length, this.cursor));
         times[this.cursor] = this.time;
         return times;
      }

      public int getCount() {
         return this.count;
      }

      public int getTotalCount() {
         return this.count + this.markedCount;
      }

      public double getAverageTime() {
         return this.count > 0 ? (double)this.time / (double)this.count : (double)0.0F;
      }

      public double getTotalAverageTime() {
         return this.count > 0 ? (double)(this.time + this.markedTime) / (double)(this.count + this.markedCount) : (double)0.0F;
      }

      protected long getMarkedTime() {
         return this.markedTime;
      }

      protected int getMarkedCount() {
         return this.markedCount;
      }
   }

   class SubSection extends LiveSection {
      private final String baseName;
      private final Section root;

      SubSection(String name, int cursor, String baseName, Section root) {
         super(name, cursor);
         this.baseName = baseName;
         this.root = root;
      }

      Section invalidate() {
         this.root.invalidate();
         return super.invalidate();
      }

      public String getBaseName() {
         return this.baseName;
      }

      public void setInfo(String info) {
         this.root.setInfo(info);
         super.setInfo(info);
      }

      Section getDelegate() {
         return this.root;
      }

      Section start() {
         this.root.start();
         return super.start();
      }

      public Section end() {
         this.root.stop();
         return super.end();
      }

      public Section next(String name) {
         super.stop();
         return this.root.next(name);
      }
   }

   static class ResultSection extends Section {
      private List<Section> sections = new ArrayList();

      ResultSection(String name) {
         super(name);
      }

      void add(Section section) {
         this.sections.add(section);
      }

      public long getTime() {
         long time = 0L;

         for(Section section : this.sections) {
            time += section.getTime();
         }

         return time;
      }

      public long getTotalTime() {
         long totalTime = 0L;

         for(Section section : this.sections) {
            totalTime += section.getTotalTime();
         }

         return totalTime;
      }

      public double getSeconds() {
         double seconds = (double)0.0F;

         for(Section section : this.sections) {
            seconds += section.getSeconds();
         }

         return seconds;
      }

      public double getTotalSeconds() {
         double totalSeconds = (double)0.0F;

         for(Section section : this.sections) {
            totalSeconds += section.getTotalSeconds();
         }

         return totalSeconds;
      }

      public long[] getTimes() {
         int cursor = 0;

         for(Section section : this.sections) {
            cursor = Math.max(cursor, section.getCursor());
         }

         long[] times = new long[cursor + 1];

         for(Section section : this.sections) {
            long[] sectionTimes = section.getTimes();

            for(int i = 0; i < sectionTimes.length; ++i) {
               times[i] += sectionTimes[i];
            }
         }

         return times;
      }

      public int getCount() {
         int count = 0;

         for(Section section : this.sections) {
            count += section.getCount();
         }

         return count;
      }

      public int getTotalCount() {
         int totalCount = 0;

         for(Section section : this.sections) {
            totalCount += section.getTotalCount();
         }

         return totalCount;
      }

      protected long getMarkedTime() {
         long markedTime = 0L;

         for(Section section : this.sections) {
            markedTime += section.getMarkedTime();
         }

         return markedTime;
      }

      protected int getMarkedCount() {
         int markedCount = 0;

         for(Section section : this.sections) {
            markedCount += section.getMarkedCount();
         }

         return markedCount;
      }

      public double getAverageTime() {
         int count = this.getCount();
         return count > 0 ? (double)this.getTime() / (double)count : (double)0.0F;
      }

      public double getTotalAverageTime() {
         int count = this.getCount();
         return count > 0 ? (double)(this.getTime() + this.getMarkedTime()) / (double)(count + this.getMarkedCount()) : (double)0.0F;
      }
   }
}
