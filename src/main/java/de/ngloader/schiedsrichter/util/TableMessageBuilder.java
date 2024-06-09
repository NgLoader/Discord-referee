package de.ngloader.schiedsrichter.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TableMessageBuilder {

	//			  Line		   Field		 Prefix	  Spaces
	protected Map<Integer, Map<Integer, TableMessageInfo>> table = new HashMap<Integer, Map<Integer, TableMessageInfo>>();

	/*
	 * LINE1: FIELD1, FIELD2, FIELD3, FIELD4
	 * LINE2: FIELD1, FIELD2, FIELD3, FIELD4
	 * LINE3: FIELD1, FIELD2, FIELD3, FIELD4
	 * LINE4: FIELD1, FIELD2, FIELD3, FIELD4
	 * 
	 * Line
	 * \/
	 * 
	 * # # # # < Field
	 * #
	 * #
	 * #
	 */

	protected Integer currentlyLine = 0;

	public TableMessageBuilder() {
		this.table.put(this.currentlyLine, new HashMap<Integer, TableMessageInfo>());
	}

	/**
	 * Add a field to the currently line
	 * 
	 * @param prefix
	 * @param content
	 * @param suffix
	 * @return
	 */
	public TableMessageBuilder addField(String prefix, String content, String suffix) {
		this.table.get(this.currentlyLine).put(this.table.get(this.currentlyLine).size(), new TableMessageInfo(prefix, content, suffix));
		return this;
	}

	/**
	 * Set a field in the currently line
	 * 
	 * @param field
	 * @param prefix
	 * @param content
	 * @param suffix
	 * @return
	 */
	public TableMessageBuilder setField(Integer field, String prefix, String content, String suffix) {
		this.table.get(this.currentlyLine).put(field, new TableMessageInfo(prefix, content, suffix));
		return this;
	}

	public TableMessageBuilder setField(Integer line, Integer field, String prefix, String content, String suffix) {
		if(!this.table.containsKey(line)) {
			this.table.put(line, new HashMap<>());
		}

		this.table.get(line).put(field, new TableMessageInfo(prefix, content, suffix));
		return this;
	}

	public TableMessageBuilder nextLine() {
		this.currentlyLine++;

		if(!this.table.containsKey(this.currentlyLine)) {
			this.table.put(this.currentlyLine, new HashMap<>());
		}
		return this;
	}

	public String build() {
		Map<Integer, String> lines = new HashMap<Integer, String>();
		Map<Integer, Integer[]> spaces = new HashMap<Integer, Integer[]>();

		for(int currentlyTable : this.table.keySet()) {
			lines.put(currentlyTable, "");

			for(int line : this.table.get(currentlyTable).keySet()) {
				if(!spaces.containsKey(line)) {
					spaces.put(line, getSpaceForLines(line));
				}

				TableMessageInfo tableMessageInfo = this.table.get(currentlyTable).get(line);

				lines.put(currentlyTable, String.format("%s%s%s%s",
						lines.get(currentlyTable),
						this.generateSpace(spaces.get(line)[0] - tableMessageInfo.getPrefix().length()) + tableMessageInfo.getPrefix(),
						this.generateSpace(spaces.get(line)[1] - tableMessageInfo.getContent().length()) + tableMessageInfo.getContent(),
						this.generateSpace(spaces.get(line)[2] - tableMessageInfo.getSuffix().length()) + tableMessageInfo.getSuffix()));
			}
		}

		return lines.entrySet().stream()
				.sorted(Comparator.comparingInt(Entry::getKey))
				.map(entry -> entry.getValue())
				.collect(Collectors.joining("\n"));
	}

	protected Integer[] getSpaceForLines(int line) {
		return new Integer[] {
				this.table.values().stream()
					.filter(map -> map.containsKey(line))
					.map(map -> map.get(line))
					.map(tableMessageInfo -> tableMessageInfo.getPrefix().length())
					.mapToInt(Integer::intValue)
					.max().getAsInt(),
				this.table.values().stream()
					.filter(map -> map.containsKey(line))
					.map(map -> map.get(line))
					.map(tableMessageInfo -> tableMessageInfo.getContent().length())
					.mapToInt(Integer::intValue)
					.max().getAsInt(),
				this.table.values().stream()
					.filter(map -> map.containsKey(line))
					.map(map -> map.get(line))
					.map(tableMessageInfo -> tableMessageInfo.getSuffix().length())
					.mapToInt(Integer::intValue)
					.max().getAsInt()
				};
	}

	protected String generateSpace(int space) {
		return space > 0 ? String.format("%" + space + "s", "") : "";
	}

	public class TableMessageInfo {

		protected String prefix;
		protected String suffix;

		protected String content;

		public TableMessageInfo(String prefix, String content, String suffix) {
			this.prefix = prefix;
			this.content = content;
			this.suffix = suffix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getContent() {
			return content;
		}

		public void setSuffix(String suffix) {
			this.suffix = suffix;
		}

		public String getSuffix() {
			return suffix;
		}
	}
}