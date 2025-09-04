package es.agonzalez.multiagent.app.dtos;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public class AIRequest {
    @NotBlank String userId;
    @NotBlank String text;
    String intent;
    Map<String,Object> params;
    
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getIntent() {
        return intent;
    }
    public void setIntent(String intent) {
        this.intent = intent;
    }
    public Map<String, Object> getParams() {
        return params;
    }
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    

    
}
