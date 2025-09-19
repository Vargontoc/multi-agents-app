package es.agonzalez.multiagent.app.dtos;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AIRequest {
    @NotBlank(message="{validation.userId.notBlank}")
    @Size(max=64, message="{validation.userId.size}")
    String userId;
    @NotBlank(message="{validation.text.notBlank}") @Size(max=500, message="{validation.text.size}") String text;
    @Size(max=64, message="{validation.intent.size}")
    String intent;
    Map<String,Object> params = new HashMap<>();
    
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
        this.params = params == null ? new HashMap<>() : params;
    }

    

    
}
