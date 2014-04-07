/*******************************************************************************
 * Copyright 2014 org.tec
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.tec.webapp.orm.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import net.sf.ezmorph.bean.MorphDynaBean;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.tec.webapp.json.JSONSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * the user entity bean
 *
 * table definition
 * my OCD doesn't like the random constraint and index naming.
 * I can also use the names for better error handling
 */
@Entity()
@Table(name = "users", catalog = "webapp",
indexes = {
    @Index(name = "user_pwd_en_idx", unique = true, columnList = "user_name, password, enabled"),
    @Index(name = "user_idx", unique = true, columnList = "user_name,") },
uniqueConstraints = {
    @UniqueConstraint(name = "user_name_uc", columnNames = { "user_name" }),
    @UniqueConstraint(name = "enail_uc", columnNames = { "email" }) })
public class User implements JSONSerializable
{
  /** the json config to filter out password when sending data to client */
  protected static final JsonConfig JSON_CONFIG = new JsonConfig();

  /**
   * register a morpher for UserRole processing
   * setup json config to filter password from client
   */
  static
  {
    JSON_CONFIG.setRootClass(User.class);
    JSON_CONFIG.setJavaPropertyFilter(new PropertyFilter()
    {
      public boolean apply(Object source, String name, Object value)
      {
        return ("password".equals(name) ? true : false);
      }
    });
  }

  /** serial guid */
  private static final long serialVersionUID = 1L;

  /** the user surrogate key */
  @Id()
  @Column(name = "user_id", unique = true, nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected int mUserId;

  /** the unique login user name */
  @Column(name = "user_name", length = 32, nullable = false)
  protected String mUserName;

  /**
   * the password for the use given user (MD5 hashed)
   * the column needs to be excluded from the user management cycle
   * there will be a reset path
   */
  @Column(name = "password", length = 32, insertable = false, updatable = false)
//  @Transient()
  protected String mPassword;

  /** the user email */
  @Column(name = "email", length = 64, nullable = false)
  protected String mEmail;

  /** whether the account is enabled */
  @Column(name = "enabled", nullable = false)
  protected boolean mEnabled;

  /** the list of roles assigned to the user */
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "user_id")
  protected List<UserRole> mUserRoles;

  /**
   * @return the userId
   */
  public int getUserId()
  {
    return mUserId;
  }

  /**
   * @param userId the userId to set
   */
  public void setUserId(int userId)
  {
    mUserId = userId;
  }

  /**
   * @return the userName
   */
  public String getUserName()
  {
    return mUserName;
  }

  /**
   * @param userName the userName to set
   */
  public void setUserName(String userName)
  {
    mUserName = userName;
  }

  /**
   * @return the password
   */
  public String getPassword()
  {
    return mPassword;
  }

  /**
   * @param password the password to set
   */
  public void setPassword(String password)
  {
    mPassword = password;
  }

  /**
   * @return the email
   */
  public String getEmail()
  {
    return mEmail;
  }

  /**
   * @param email the email to set
   */
  public void setEmail(String email)
  {
    mEmail = email;
  }

  /**
   * @return the enabled
   */
  public boolean getEnabled()
  {
    return mEnabled;
  }

  /**
   * @param enabled the enabled to set
   */
  public void setEnabled(boolean enabled)
  {
    mEnabled = enabled;
  }

  /**
   * @return the userRoles
   */
  public List<UserRole> getUserRoles()
  {
    return mUserRoles;
  }

  /**
   * @param userRoles the userRoles to set
   */
  public void setUserRoles(List<UserRole> userRoles)
  {
    mUserRoles = userRoles;
  }

  /**
   * {@inheritDoc}
   */
  @Override()
  public String toString()
  {
    return new ToStringBuilder(this).appendSuper(super.toString())
        .append("mUserId", mUserId)
        .append("mUserName", mUserName)
        .append("mEmail", mEmail)
        .append("mEnabled", mEnabled)
        .append("mUserRoles", mUserRoles).toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override()
  public String toJSON()
  {
    return JSONObject.fromObject(this, JSON_CONFIG).toString();
  }

  /**
   * helper function to convert json to user object
   * @param json the json definition of a user
   * @return the user instance
   */
  public static User jsonToUser(String json)
  {
    User u = (User) JSONObject.toBean(JSONObject.fromObject(json), User.class);

    morphUserRoles(u);

    return u;
  }

  /**
   * converts a json string into a collection of users
   * @param json the json string [{...},{...}]
   * @return the collection of users
   */
  @SuppressWarnings("unchecked")
  public static Collection<User> jsonArrayToUsers(String json)
  {
    Collection<User> users = (Collection<User>) JSONArray.toCollection(JSONArray.fromObject(json), User.class);

    for (User u : users)
    {
      morphUserRoles(u);
    }
    return users;
  }

  /**
   * post process the userRole list since json-lib doesn't
   * handle collection properties in the top level conversion
   * @param user the user to morph the userRoles
   */
  protected static void morphUserRoles(User user)
  {
    List<UserRole> roles = new ArrayList<UserRole>();
    for (Object o : user.getUserRoles())
    {
      MorphDynaBean dynaBean = (MorphDynaBean) o;
      UserRole ur = new UserRole();
      ur.setUser(user);
      ur.setRole(RoleType.fromName((String) dynaBean.get("Role")));
      roles.add(ur);
    }
    user.setUserRoles(roles);
  }
}