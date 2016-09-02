package in.strollup.fb.contract;

import java.util.List;

import javax.annotation.Generated;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * 
 * @author Ayoub50
 *
 */
@Generated("org.jsonschema2pojo")
public class QuickReply {

	@SerializedName("content_type")
	@Expose
	private String content_type;
	@SerializedName("title")
	@Expose
	private String title;
	@SerializedName("text")
	@Expose
	private String text;
	@SerializedName("payload")
	@Expose
	private String payload;
	@SerializedName("quick_replies")
	@Expose
	private List<QuickReply> quickReplies;
	/**
	 * 
	 * @return The type
	 */
	public String getType() {
		return content_type;
	}

	/**
	 * 
	 * @param type
	 *            The type
	 */
	public void setType(String content_type) {
		this.content_type = content_type;
	}

	/**
	 * 
	 * @return The title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 
	 * @param title
	 *            The title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * 
	 * @return The payload
	 */
	public String getPayload() {
		return payload;
	}

	/**
	 * 
	 * @param payload
	 *            The payload
	 */
	public void setPayload(String payload) {
		this.payload = payload;
	}

	public List<QuickReply> getQuickReplies() {
		return quickReplies;
	}

	public void setQuickReplies(List<QuickReply> quickReplies) {
		this.quickReplies = quickReplies;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(content_type).append(title).append(payload).toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof Button) == false) {
			return false;
		}
		QuickReply rhs = ((QuickReply) other);
		return new EqualsBuilder().append(content_type, rhs.content_type).append(title, rhs.title).append(payload, rhs.payload)
				.isEquals();
	}

}
