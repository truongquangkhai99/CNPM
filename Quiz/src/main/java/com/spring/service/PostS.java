package com.spring.service;

import java.util.List;

import com.spring.domain.Account;
import com.spring.domain.Comment;
import com.spring.domain.Post;

public interface PostS {
	/**
	 * Lấy danh sách những bình luận từ mã bài đăng
	 * 
	 * @param idPost
	 * @return list Comment
	 */
	public List<Comment> getListCommentByIdPost(int idPost);

	/**
	 * lấy nội dung bài đăng từ id bài đăng
	 * 
	 * @param idPost
	 * @return Post object
	 */
	public Post getPostById(int idPost);

	/**
	 * xóa bài đăngtrong nhóm đưới quyền của id acc
	 * 
	 * @param idPost
	 * @param idAcc
	 * @return thành công or thất bại
	 */

	public boolean deletePost(int idPost, int idAcc);

	public boolean likePostFromAccount(int idPost, Integer idAcc);

	public List<Account> getListAccountLike(int idPost);

}