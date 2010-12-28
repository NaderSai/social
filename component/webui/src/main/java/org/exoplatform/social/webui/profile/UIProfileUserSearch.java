/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.webui.URLUtils;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Searches users in profile by user name and some other filter condition.<br>
 * - Search action is listened and information for search user is processed.<br>
 * - After users is requested is returned, the search process is completed, -
 * Search event is broadcasted to the form that added search form as child.<br>
 * Author : hanhvi hanhvq@gmail.com
 * Sep 25, 2009
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "classpath:groovy/social/webui/profile/UIProfileUserSearch.gtmpl",
  events = {
    @EventConfig(listeners = UIProfileUserSearch.SearchActionListener.class)
  }
)
public class UIProfileUserSearch extends UIForm {

  /** SEARCH. */
  final public static String SEARCH                    = "Search";

  /** USER CONTACT. */
  final public static String USER_CONTACT              = "name";

  /** DEFAULT GENDER. */
  final public static String GENDER_DEFAULT            = "Gender";

  /** MALE. */
  final public static String MALE                      = "male";

  /** FEMALE. */
  final public static String FEMALE                    = "female";

  /** REGEX FOR SPLIT STRING */
  final public static String REG_FOR_SPLIT             = "[^_A-Za-z0-9-.\\s[\\n]]";

  /** PATTERN FOR CHECK RIGHT INPUT VALUE */
  final static String        RIGHT_INPUT_PATTERN       = "^[\\p{L}][\\p{L}._\\- \\d]+$";

  /** REGEX EXPRESSION OF POSITION FIELD. */
  final public static String POSITION_REGEX_EXPRESSION = "^\\p{L}[\\p{L}\\d._,\\s]+\\p{L}$";

  /** ADD PREFIX TO ENSURE ALWAY RIGHT THE PATTERN FOR CHECKING */
  final static String        PREFIX_ADDED_FOR_CHECK    = "PrefixAddedForCheck";

  /** The im is used for stores IdentityManager instance. */
  IdentityManager            im                        = null;

  /** The rm is used for stores RelationshipManager instance. */
  RelationshipManager        rm                        = null;

  /** Stores current identity. */
  Identity                   currIdentity              = null;

  /** List used for identities storage. */
  private List<Identity>     identityList              = null;

  /** Stores selected character when search by alphabet */
  String                     selectedChar              = null;

  /** Used stores filter information. */
  ProfileFilter              profileFilter             = null;

  /** Number of identities. */
  long identitiesCount;
  
  /** The flag notifies a new search when clicks search icon or presses enter.  */
  private boolean isNewSearch;
  
  /**
   * Sets list identity.
   *
   * @param identityList <code>List</code>
   */
  public void setIdentityList(List<Identity> identityList) {
    this.identityList = identityList;
  }

  /**
   * Gets identity list result search.
   *
   * @return List of identity.
   * @throws Exception
   */
  public List<Identity> getIdentityList() throws Exception {
    return identityList;
  }

  /**
   * Gets selected character when search by alphabet.
   *
   * @return The selected character.
   */
  public String getSelectedChar() {
    return selectedChar;
  }

  /**
   * Sets selected character to variable.
   *
   * @param selectedChar <code>char</code>
   */
  public void setSelectedChar(String selectedChar) {
    this.selectedChar = selectedChar;
  }

  /**
   * Gets filter object.
   *
   * @return The object that contain filter information.
   */
  public ProfileFilter getProfileFilter() {
    return profileFilter;
  }

  /**
   * Sets filter object.
   *
   * @param profileFilter <code>Object<code>
   */
  public void setProfileFilter(ProfileFilter profileFilter) {
    this.profileFilter = profileFilter;
  }

  /**
   * Initializes user search form fields. Initials and adds components as
   * children to search form.
   *
   * @throws Exception
   */
  public UIProfileUserSearch() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(GENDER_DEFAULT));
    options.add(new SelectItemOption<String>(MALE));
    options.add(new SelectItemOption<String>(FEMALE));

    addUIFormInput(new UIFormStringInput(SEARCH, USER_CONTACT, USER_CONTACT));
    addUIFormInput(new UIFormStringInput(Profile.POSITION, Profile.POSITION, Profile.POSITION));
    addUIFormInput(new UIFormStringInput(Profile.EXPERIENCES_SKILLS, Profile.EXPERIENCES_SKILLS, Profile.EXPERIENCES_SKILLS));
    addUIFormInput(new UIFormSelectBox(Profile.GENDER, Profile.GENDER, options));
  }

  /**
   * Gets current identity.
   *
   * @return identity of current user.
   * @throws Exception
   */
  public Identity getCurrentIdentity() throws Exception {
    IdentityManager im = getIdentityManager();
    return im.getOrCreateIdentity(OrganizationIdentityProvider.NAME, getCurrentUserName());
  }

  /**
   * Gets current viewer identity.
   *
   * @return the identity of person who is viewed by another.
   * @throws Exception
   */
  public Identity getCurrentViewerIdentity() throws Exception {
    IdentityManager im = getIdentityManager();
    Identity identity = null;
    identity = im.getOrCreateIdentity(OrganizationIdentityProvider.NAME, getCurrentViewerUserName());
    if (identity == null) {
      return im.getOrCreateIdentity(OrganizationIdentityProvider.NAME, getCurrentUserName());
    }

    return identity;
  }

  /**
   * Get current user name.
   *
   * @return
   */
  public String getCurrentUserName() {
    RequestContext context = RequestContext.getCurrentInstance();
    return context.getRemoteUser();
  }

  /**
   * Get current rest context name.
   *
   * @return
   */
  protected String getRestContextName() {
    return PortalContainer.getCurrentRestContextName();
  }
  
  /**
   * Gets number of identities.
   * 
   * @return Number of identities.
   */
  private long getIdentitiesCount() {
    if (identitiesCount == 0L) {
      identitiesCount = getIdentityManager().getIdentitiesCount(OrganizationIdentityProvider.NAME);  
    }
    
    return identitiesCount;
  }
  
  /**
   * Listens to search event from search form, then processes search condition
   * and set search result to the result variable.<br>
   * - Gets user name and other filter information from request.<br>
   * - Searches user that matches the condition.<br>
   * - Sets matched users into result list.<br>
   */
  static public class SearchActionListener extends EventListener<UIProfileUserSearch> {
    @Override
    public void execute(Event<UIProfileUserSearch> event) throws Exception {
      WebuiRequestContext ctx = event.getRequestContext();
      UIProfileUserSearch uiSearch = event.getSource();
      String charSearch = ctx.getRequestParameter(OBJECTID);
      List<Identity> identitiesSearchResult;
      List<Identity> identities;
      IdentityManager idm = uiSearch.getIdentityManager();
      ProfileFilter filter = new ProfileFilter();
      uiSearch.invokeSetBindingBean(filter);
      ResourceBundle resApp = ctx.getApplicationResourceBundle();

      String defaultNameVal = resApp.getString(uiSearch.getId() + ".label.Name");
      String defaultPosVal = resApp.getString(uiSearch.getId() + ".label.Position");
      String defaultSkillsVal = resApp.getString(uiSearch.getId() + ".label.Skills");
      String defaultGenderVal = resApp.getString(uiSearch.getId() + ".label.AllGender");
      try {
        uiSearch.setSelectedChar(charSearch);
        if (charSearch != null) { // search by alphabet
          ((UIFormStringInput) uiSearch.getChildById(SEARCH)).setValue(defaultNameVal);
          ((UIFormStringInput) uiSearch.getChildById(Profile.POSITION)).setValue(defaultPosVal);
          ((UIFormStringInput) uiSearch.getChildById(Profile.EXPERIENCES_SKILLS)).setValue(defaultSkillsVal);
          ((UIFormStringInput) uiSearch.getChildById(Profile.GENDER)).setValue(defaultGenderVal);
          filter.setName(charSearch);
          filter.setPosition("");
          filter.setGender("");
          if ("All".equals(charSearch)) {
            filter.setName("");
          }
          identitiesSearchResult = idm.getIdentitiesFilterByAlphaBet(OrganizationIdentityProvider.NAME,
                                                                     filter, 0, uiSearch.getIdentitiesCount());
          uiSearch.setIdentityList(identitiesSearchResult);
        } else {
          if (!isValidInput(filter)) { // is invalid condition input
            uiSearch.setIdentityList(new ArrayList<Identity>());
          } else {
            if ((filter.getName() == null) || filter.getName().equals(defaultNameVal)) {
              filter.setName("");
            }
            if ((filter.getPosition() == null) || filter.getPosition().equals(defaultPosVal)) {
              filter.setPosition("");
            }
            if ((filter.getSkills() == null) || filter.getSkills().equals(defaultSkillsVal)) {
              filter.setSkills("");
            }
            if (filter.getGender().equals(defaultGenderVal)) {
              filter.setGender("");
            }

            String skills = null;

            // uiSearch.setSelectedChar(charSearch);

            identitiesSearchResult = idm.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME,
                                                                      filter, 0, uiSearch.getIdentitiesCount());
            uiSearch.setIdentityList(identitiesSearchResult);

            // Using regular expression for search
            skills = filter.getSkills();
            if (skills.length() > 0) {
              skills = ((skills == "") || (skills.length() == 0)) ? "*" : skills;
              skills = (skills.charAt(0) != '*') ? "*" + skills : skills;
              skills = (skills.charAt(skills.length() - 1) != '*') ? skills += "*" : skills;
              skills = (skills.indexOf("*") >= 0) ? skills.replace("*", ".*") : skills;
              skills = (skills.indexOf("%") >= 0) ? skills.replace("%", ".*") : skills;
              Pattern.compile(skills);
              identities = uiSearch.getIdentitiesBySkills(skills, identitiesSearchResult);
              uiSearch.setIdentityList(identities);
            }
          }
        }
        uiSearch.setNewSearch(true);
      } catch (Exception e) {
        uiSearch.setIdentityList(new ArrayList<Identity>());
      }
      Event<UIComponent> searchEvent = uiSearch.<UIComponent> getParent()
                                               .createEvent(SEARCH, Event.Phase.DECODE, ctx);
      if (searchEvent != null) {
        searchEvent.broadcast();
      }
    }

    /**
     * Checks input values follow regular expression.
     *
     * @param input <code>Object</code>
     * @return true if the input is properly to regular expression else return
     *         false.
     */
    private boolean isValidInput(ProfileFilter input) {
      // Check contact name
      String contactName = input.getName();
      // Eliminate '*' and '%' character in string for checking
      String contactNameForCheck = null;
      if (contactName != null) {
        contactNameForCheck = contactName.trim().replace("*", "");
        contactNameForCheck = contactNameForCheck.replace("%", "");
        // Make sure string for checking is started by alphabet character
        contactNameForCheck = PREFIX_ADDED_FOR_CHECK + contactNameForCheck;
        if (!contactNameForCheck.matches(RIGHT_INPUT_PATTERN))
          return false;
      }

      return true;
    }
  }

  /**
   * Filter identity follow skills information.
   *
   * @param skills <code>String</code>
   * @param identities <code>Object</code>
   * @return List of identities that has skills information match.
   */
  @SuppressWarnings("unchecked")
  private List<Identity> getIdentitiesBySkills(String skills, List<Identity> identities) {
    List<Identity> identityLst = new ArrayList<Identity>();
    String prof = null;
    ArrayList<HashMap<String, Object>> experiences;
    String skill = skills.trim().toLowerCase();

    if (identities.size() == 0)
      return identityLst;

    for (Identity id : identities) {
      Profile profile = id.getProfile();
      experiences = (ArrayList<HashMap<String, Object>>) profile.getProperty(Profile.EXPERIENCES);
      if (experiences == null)
        continue;
      for (HashMap<String, Object> exp : experiences) {
        prof = (String) exp.get(Profile.EXPERIENCES_SKILLS);
        if (prof == null)
          continue;
        Pattern p = Pattern.compile(REG_FOR_SPLIT);
        String[] items = p.split(prof);
        for (String item : items) {
          if (item.toLowerCase().matches(skill)) {
            identityLst.add(id);
            break;
          }
        }
      }
    }

    return GetUniqueIdentities(identityLst);
  }

  /**
   * Get identity manager.
   *
   * @return an IdentityManager instance.
   */
  private IdentityManager getIdentityManager() {
    if (im == null) {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      im = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
    }
    return im;
  }

  /**
   * Gets current viewer user name.
   *
   * @return the name of user who viewed by another.
   */
  private String getCurrentViewerUserName() {
    String username = URLUtils.getCurrentUser();
    if (username != null)
      return username;

    PortalRequestContext portalRequest = Util.getPortalRequestContext();
    return portalRequest.getRemoteUser();
  }

  /**
   * Unions to collection to make one collection.
   *
   * @param identities1 <code>Object</code>
   * @param identities2 <code>Object</code>
   * @return One new collection that the union result of two input collection.
   */
  private static Collection<Identity> Union(Collection<Identity> identities1,
                                            Collection<Identity> identities2) {
    Set<Identity> identities = new HashSet<Identity>(identities1);
    identities.addAll(new HashSet<Identity>(identities2));
    return new ArrayList<Identity>(identities);
  }

  /**
   * Gets unique identities from one collection of identities.
   *
   * @param identities <code>Object</code>
   * @return one list that contains unique identities.
   */
  private static ArrayList<Identity> GetUniqueIdentities(Collection<Identity> identities) {
    return (ArrayList<Identity>) Union(identities, identities);
  }

  public boolean isNewSearch() {
    return isNewSearch;
  }

  public void setNewSearch(boolean isNewSearch) {
    this.isNewSearch = isNewSearch;
  }
}
