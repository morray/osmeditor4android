package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.rtree.BoundedObject;

/**
 * Relation represents an OSM relation element which essentially is a collection of other OSM elements.
 * 
 * @author simon
 *
 */
public class Relation extends OsmElement implements BoundedObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1104911642016294265L;

	protected final ArrayList<RelationMember> members;

	public static final String NAME = "relation";

	public static final String MEMBER = "member";
	
	Relation(final long osmId, final long osmVersion, final byte status) {
		super(osmId, osmVersion, status);
		members = new ArrayList<RelationMember>();
	}

	protected void addMember(final RelationMember member) {
		members.add(member);
	}

	/**
	 * Return complete list of relation members
	 * @return
	 */
	public List<RelationMember> getMembers() {
		return members;
	}
	
	/**
	 * Return first relation member element for this OSM element
	 * Note: if the element is present more than once you will only get one
	 * @param e
	 * @return
	 */
	public RelationMember getMember(OsmElement e) {
		for (int i = 0; i < members.size(); i++) {
			RelationMember member = members.get(i);
			if (member.getElement() == e) {
				return member;
			}
		}
		return null;
	}
	
	/**
	 * Return all relation member elements for this OSM element
	 * @param e
	 * @return
	 */
	public List<RelationMember> getAllMembers(OsmElement e) {
		ArrayList<RelationMember> result = new ArrayList<RelationMember>();
		for (int i = 0; i < members.size(); i++) {
			RelationMember member = members.get(i);
			if (member.getElement() == e) {
				result.add(member);
			}
		}
		return result;
	}
	
	/**
	 * Return first relation member element for this OSM element
	 * Note: if the element is present more than once you will only get ont
	 * @param type
	 * @param id
	 * @return
	 */
	public RelationMember getMember(String type, long id) {
		for (int i = 0; i < members.size(); i++) {
			RelationMember member = members.get(i);
			if (member.getRef() == id && member.getType().equals(type)) {
				return member;
			}
		}
		return null;
	}

	public int getPosition(RelationMember e) {
		return members.indexOf(e);
	}
	
	/**
	 * 
	 * @return list of members allowing {@link Iterator#remove()}.
	 */
	Iterator<RelationMember> getRemovableMembers() {
		return members.iterator();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String toString() {
//		String res = super.toString();
//		for (Map.Entry<String, String> tag : tags.entrySet()) {
//			res += "\t" + tag.getKey() + "=" + tag.getValue();
//		}
//		for (RelationMember m:members) {
//			res += "\t" + m.toString();
//		}
//		return res;
		return getDescription();
	}

	@Override
	public void toXml(final XmlSerializer s, final Long changeSetId) throws IllegalArgumentException,
			IllegalStateException, IOException {
		s.startTag("", "relation");
		s.attribute("", "id", Long.toString(osmId));
		if (changeSetId != null) s.attribute("", "changeset", Long.toString(changeSetId));
		s.attribute("", "version", Long.toString(osmVersion));

		for (RelationMember member : members) {
			s.startTag("", "member");
			s.attribute("", "type", member.getType());
			s.attribute("", "ref", Long.toString(member.getRef()));
			s.attribute("", "role", member.getRole());
			s.endTag("", "member");
		}

		tagsToXml(s);
		s.endTag("", "relation");
	}
	
	@Override
	public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException,
			IllegalStateException, IOException {
		s.startTag("", "relation");
		s.attribute("", "id", Long.toString(osmId));
		if (state == OsmElement.STATE_DELETED) {
			s.attribute("", "action", "delete");
		} else if (state == OsmElement.STATE_CREATED || state == OsmElement.STATE_MODIFIED) {
			s.attribute("", "action", "modify");
		}
		s.attribute("", "version", Long.toString(osmVersion));
		s.attribute("", "visible", "true");
		
		for (RelationMember member : members) {
			s.startTag("", "member");
			s.attribute("", "type", member.getType());
			s.attribute("", "ref", Long.toString(member.getRef()));
			s.attribute("", "role", member.getRole());
			s.endTag("", "member");
		}

		tagsToXml(s);
		s.endTag("", "relation");
	}
	
	public boolean hasMember(final RelationMember member) {
		return members.contains(member);
	}

	/**
	 * Completely remove member from relation (even if present more than once)
	 * Does not update backlink
	 * @param member
	 */
	protected void removeMember(final RelationMember member) {
		while (members.remove(member)) {
		}
	}

	protected void appendMember(final RelationMember refMember, final RelationMember newMember) {
		if (members != null && members.size() > 0 && members.get(0) == refMember) {
			members.add(0, newMember);
		} else if (members != null && members.get(members.size() - 1) == refMember) {
			members.add(newMember);
		}
	}

	protected void addMemberAfter(final RelationMember memberBefore, final RelationMember newMember) {
		members.add(members.indexOf(memberBefore) + 1, newMember);
	}
	
	protected void addMember(int pos, final RelationMember newMember) {
		if (pos < 0 || pos > members.size()) {
			pos = members.size(); // append
		}
		members.add(pos, newMember);
	}
	
	/**
	 * Adds multiple elements to the relation in the order in which they appear in the list.
	 * They can be either prepended or appended to the existing nodes.
	 * @param newMembers a list of new members
	 * @param atBeginning if true, nodes are prepended, otherwise, they are appended
	 */
	protected void addMembers(List<RelationMember> newMembers, boolean atBeginning) {
		if (atBeginning) {
			members.addAll(0, newMembers);
		} else {
			members.addAll(newMembers);
		}
	}
	
	public ArrayList <RelationMember> getMembersWithRole(String role) {
		ArrayList <RelationMember> rl = new ArrayList<RelationMember>();
		for (RelationMember rm : members) {
			Log.d("Relation", "getMembersWithRole " + rm.getRole());
			if (role.equals(rm.getRole())) {
				rl.add(rm);
			}
		}
		return rl;
	}
	
	/**
	 * Replace an existing member in a relation with a different member.
	 * @param existing The existing member to be replaced.
	 * @param newMember The new member.
	 */
	void replaceMember(RelationMember existing, RelationMember newMember) {
		int idx;
		while ((idx = members.indexOf(existing)) != -1) {
			members.set(idx, newMember);
		}
	}
	
	/**
	 * Replace all existing members in a relation.
	 * @param existing The existing member to be replaced.
	 * @param newMember The new member.
	 */
	protected void replaceMembers(Collection<RelationMember> newMembers) {
		members.clear();
		members.addAll(newMembers);
	}

	/**
	 * rough implementation for now
	 */
	@Override
	public String getDescription() {
		return getDescription(null);
	}
	
	@Override
	public String getDescription(Context ctx) {
		String description = "";
		PresetItem p = null;
		if (ctx != null) {
			p = Preset.findBestMatch(Application.getCurrentPresets(ctx),tags);
		} 
		if (p!=null) {
			description = p.getTranslatedName();
		} else {
			String type = getTagWithKey(Tags.KEY_TYPE);
			if (type != null && !type.equals("")){
				description = type;
				if (type.equals(Tags.VALUE_RESTRICTION)) {
					String restriction = getTagWithKey(Tags.VALUE_RESTRICTION);
					if (restriction != null) {
						description = restriction + " " + description;
					}
				} else if (type.equals(Tags.VALUE_ROUTE)) {
					String route = getTagWithKey(Tags.VALUE_ROUTE);
					if (route != null) {
						description = route + " " + description ;
					}
				} else if (type.equals(Tags.VALUE_MULTIPOLYGON)) {
					String b = getTagWithKey(Tags.KEY_BOUNDARY);
					if (b != null) {
						description = b + " " + Tags.KEY_BOUNDARY + " " + description ;
					} else {
						String l = getTagWithKey(Tags.KEY_LANDUSE);
						if (l != null) {
							description = Tags.KEY_LANDUSE + " " + l + " " + description ;
						} else {
							String n = getTagWithKey(Tags.KEY_NATURAL);
							if (n != null) {
								description = Tags.KEY_NATURAL + " " + n + " " + description ;
							}
						}
					}
				}
			} else  {
				if (ctx == null) {
					description = "unset relation type"; // fallback so that we have something to display
				} else {
					description = ctx.getResources().getString(R.string.unset_relation_type);
				}
			}
		}
		String name = getTagWithKey(Tags.KEY_NAME);
		if (name != null) {
			description = description + " " + name;
		} else {
			description = description + " #" + osmId;
		}
		return description;
	}

	
	/**
	 * Test if the relation has a problem.
	 * @return true if the relation has a problem, false if it doesn't.
	 */
	@Override
	protected boolean calcProblem() {
		String type = getTagWithKey(Tags.KEY_TYPE);
		if (type==null || type.equals("")) {
			return true;
		}
		return super.calcProblem();
	}
	
	@Override
	public String describeProblem() {
		String superProblem = super.describeProblem();
		String relationProblem = "";
		String type = getTagWithKey(Tags.KEY_TYPE);
		if (type==null || type.equals("")) {
			relationProblem = Application.mainActivity.getString(R.string.toast_notype);
		}
		if (!superProblem.equals("")) 
			return superProblem + (!relationProblem.equals("") ? "\n" + relationProblem : "");
		else
			return relationProblem;
	}

	@Override
	public ElementType getType() {
		return getType(tags);
	}

	@Override
	public ElementType getType(Map<String,String> tags) {
		if (hasTag(tags, Tags.KEY_TYPE,Tags.VALUE_MULTIPOLYGON) || hasTag(tags, Tags.KEY_TYPE,Tags.VALUE_BOUNDARY)) {
			return ElementType.AREA;
		}
		return ElementType.RELATION;
	}
	
	/**
	 * return a list of the downloaded elements
	 * @return
	 */
	public ArrayList<OsmElement> getMemberElements() {
		ArrayList<OsmElement> result = new ArrayList<OsmElement>();
		for (RelationMember rm:getMembers()) {
			if (rm.getElement()!=null)
				result.add(rm.getElement());
		}
		return result;
	}

	@Override
	public BoundingBox getBounds() {
		// NOTE this will only return a bb covering the downloaded elements 
		BoundingBox result = null;
		for (RelationMember rm:members) {
			OsmElement e = rm.getElement();
			if (e != null) {
				if (result == null) {
					result = e.getBounds();
				} else {
					result.union(e.getBounds());
				}
			}
		}
		return result;
	}
	
	
}
