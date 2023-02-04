import axios from "axios";
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import BoardComment from "./BoardComment";

function BoardDetail() {
  const [board, setBoard] = useState({});
  const [commentValue, setCommentValue] = useState("");

  const movePage = useNavigate();

  const params = new URLSearchParams(window.location.search);
  const id = parseInt(params.get("id"));

  useEffect(() => {
    axios.get("http://3.35.149.202:80/api/board/" + id).then((response) => {
      setBoard(response.data.findBoard);
    });
  }, [id]);

  const goBack = () => {
    movePage("/community/list/");
  };

  const addComment = (event) => {
    event.preventDefault();
    axios.get("http://3.35.149.202:80/api/board/" + id).then((response) => {
      setBoard(response.data.findBoard);
    });
    console.log(commentValue);
  };

  const addSubCommentConfirm = (event) => {
    event.preventDefault();
    console.log("대댓글 등록");
    axios.get("http://3.35.149.202:80/api/board/" + id).then((response) => {
      setBoard(response.data.findBoard);
    });
  };

  const getValue = (event) => {
    setCommentValue(event.target.value);
  };

  const comments = board.boardComments;

  return (
    <div className="w-4/5 min-h-screen p-2 m-auto bg-slate-400">
      <div className="flex flex-col items-center border-b">
        <span className="text-[#FA9E13] font-semibold">{board.movieId}</span>
        <span className="text-2xl font-extrabold">{board.title}</span>
        <div className="flex w-full">
          <span className="flex-1 cursor-pointer" onClick={goBack}>
            &lt; 이전으로
          </span>
          <span className="flex-1 text-center">{board.dateTime}</span>
          <span className="flex-1"></span>
        </div>
      </div>
      <div className="p-2 border-b">
        <p>{board.content}</p>
      </div>

      <span>댓글</span>
      <div className="p-2 border-b-2 border-black bg-slate-300">
        <form className="flex gap-2" onSubmit={addComment}>
          <input
            type="text"
            className="flex-1 p-2 rounded"
            placeholder="내용을 입력해 주세요"
            onChange={getValue}
          ></input>
          <button className="w-20 h-20 bg-[#FA9E13] rounded text-white">
            등록
          </button>
        </form>
      </div>
      {comments &&
        comments.map((comment) => (
          <BoardComment
            id={comment.id}
            groupId={comment.groupId}
            isNestedComment={comment.isNestedComment}
            writeDate={comment.writeDate}
            boardCommentStatus={comment.boardCommentStatus}
            content={comment.content}
            writer={comment.writer}
            addSubCommentConfirm={addSubCommentConfirm}
          />
        ))}
    </div>
  );
}

export default BoardDetail;
