package in.strollup.fb.contract;

import java.util.List;
import javax.annotation.Generated;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Message {

	@SerializedName("mid")
	@Expose
	private String mid;
	@SerializedName("seq")
	@Expose
	private Long seq;
	@SerializedName("text")
	@Expose
	private String text;
	@SerializedName("sticker_id")
	@Expose
	private Long stickerId;
	@SerializedName("is_echo")
	@Expose
	private Boolean echo;
	@SerializedName("attachments")
	@Expose
	private List<Attachment> attachments;

	@SerializedName("attachment")
	@Expose
	private Attachment attachment;
	
	@SerializedName("quick_replies")
	@Expose
	private List<QuickReply> quick_replies;
	
	public List<QuickReply> getQuickReplies() {
		return quick_replies;
	}
	public void setQuickReplies(List<QuickReply> quick_replies) {
		this.quick_replies = quick_replies;
	}
	public Attachment getAttachment() {
		return attachment;
	}

	public void setAttachment(Attachment attachment) {
		this.attachment = attachment;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	/**
	 * 
	 * @return The mid
	 */
	public String getMid() {
		return mid;
	}

	/**
	 * 
	 * @param mid
	 *            The mid
	 */
	public void setMid(String mid) {
		this.mid = mid;
	}

	/**
	 * 
	 * @return The seq
	 */
	public Long getSeq() {
		return seq;
	}

	/**
	 * 
	 * @param seq
	 *            The seq
	 */
	public void setSeq(Long seq) {
		this.seq = seq;
	}

	/**
	 * 
	 * @return The stickerId
	 */
	public Long getStickerId() {
		return stickerId;
	}

	/**
	 * 
	 * @param stickerId
	 *            The sticker_id
	 */
	public void setStickerId(Long stickerId) {
		this.stickerId = stickerId;
	}

	/**
	 * 
	 * @return The attachments
	 */
	public List<Attachment> getAttachments() {
		return attachments;
	}

	public Boolean isEcho() {
		return echo;
	}
	public void setEcho(Boolean echo) {
		this.echo = echo;
	}
	public List<QuickReply> getQuick_replies() {
		return quick_replies;
	}
	public void setQuick_replies(List<QuickReply> quick_replies) {
		this.quick_replies = quick_replies;
	}
	/**
	 * 
	 * @param attachments
	 *            The attachments
	 */
	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(mid).append(seq).append(stickerId).append(attachments).append(quick_replies).toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof Message) == false) {
			return false;
		}
		Message rhs = ((Message) other);
		return new EqualsBuilder().append(mid, rhs.mid).append(seq, rhs.seq).append(stickerId, rhs.stickerId)
				.append(attachments, rhs.attachments).append(quick_replies, rhs.quick_replies).isEquals();
	}

}
