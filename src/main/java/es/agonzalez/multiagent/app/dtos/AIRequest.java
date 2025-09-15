package es.agonzalez.multiagent.app.dtos;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AIRequest {
    @Size(max=64, message="userId demasiado largo (max 64)")
    String userId;
    @NotBlank(message="text no puede estar vacío") @Size(max=500, message="text excede 500 chars") String text;
    @Size(max=64, message="intent demasiado largo (max 64)")
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
