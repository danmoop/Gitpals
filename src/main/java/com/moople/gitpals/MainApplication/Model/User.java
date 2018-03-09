package com.moople.gitpals.MainApplication.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "users")
public class User
{
    @Id
    private String id;

    private String username;
    private String GithubAccountLink;
    private String country;
    private Map<String, Boolean> languagesKnows;

    public User(String username, String GithubAccountLink, String country, Map<String, Boolean> languagesKnows)
    {
        this.username = username;
        this.GithubAccountLink = GithubAccountLink;
        this.country = country;
        this.languagesKnows = languagesKnows;
    }

    public User(){}

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getGithubAccountLink()
    {
        return GithubAccountLink;
    }

    public void setGithubAccountLink(String githubAccountLink)
    {
        GithubAccountLink = githubAccountLink;
    }

    public String getCountry()
    {
        return country;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public Map<String, Boolean> getLanguagesKnows()
    {
        return languagesKnows;
    }

    public void addLanguage(String language)
    {
        languagesKnows.put(language, true);
    }

    public void deleteLanguage(String language)
    {
        languagesKnows.remove(language);
    }
}